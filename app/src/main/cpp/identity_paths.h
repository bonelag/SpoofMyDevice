#pragma once

#include <map>
#include <string>
#include <string_view>
#include <vector>

namespace smd {

using IdentityPathMap = std::map<std::string, std::string, std::less<>>;

inline bool starts_with(std::string_view path, std::string_view prefix) noexcept {
    return path.size() >= prefix.size() && path.compare(0, prefix.size(), prefix) == 0;
}

inline bool subtree(std::string_view path, std::string_view root) noexcept {
    return starts_with(path, root) && (path.size() == root.size() || path[root.size()] == '/');
}

// All virtual identity files live in these OS trees. Ordinary absolute app,
// APK, font, database and library paths bypass snapshot locks and allocations.
// Relative paths and root-changing dot segments still need normal resolution.
inline bool may_redirect_identity_path(const char *path) noexcept {
    if (path == nullptr || path[0] == '\0') return false;
    std::string_view value(path);
    if (value[0] != '/') return true;
    if (subtree(value, "/proc") || subtree(value, "/sys") || subtree(value, "/dev")) return true;
    if (value.size() > 1 && value[1] == '/') return true;
    for (size_t index = 1; index < value.size(); ++index) {
        if (value[index - 1] != '/' || value[index] != '.') continue;
        if (index == 1 && (index + 1 == value.size() || value[index + 1] == '/')) return true;
        if (index + 1 < value.size() && value[index + 1] == '.'
            && (index + 2 == value.size() || value[index + 2] == '/')) return true;
    }
    return false;
}

inline bool normalized_identity_path(std::string_view path) noexcept {
    if (path.empty() || path[0] != '/') return false;
    if (path.size() == 1) return true;
    if (path.back() == '/') return false;
    for (size_t index = 1; index < path.size(); ++index) {
        if (path[index - 1] != '/') continue;
        if (path[index] == '/') return false;
        if (path[index] == '.' && (index + 1 == path.size() || path[index + 1] == '/'
            || (path[index + 1] == '.' && (index + 2 == path.size() || path[index + 2] == '/')))) return false;
    }
    return true;
}

inline std::string normalize_identity_path(std::string_view path) {
    if (normalized_identity_path(path)) return std::string(path);
    std::vector<std::string_view> parts;
    size_t begin = 0;
    while (begin < path.size()) {
        size_t slash = path.find('/', begin);
        auto part = path.substr(begin, slash == std::string_view::npos ? path.size() - begin : slash - begin);
        if (part == "..") { if (!parts.empty()) parts.pop_back(); }
        else if (!part.empty() && part != ".") parts.push_back(part);
        if (slash == std::string_view::npos) break;
        begin = slash + 1;
    }
    std::string result;
    result.reserve(path.size());
    for (auto part : parts) { result += '/'; result.append(part.data(), part.size()); }
    return result.empty() ? "/" : result;
}

inline const IdentityPathMap::value_type *identity_path_parent(const IdentityPathMap &paths,
    std::string_view path, bool logical) {
    auto candidate = path;
    while (!candidate.empty()) {
        // Proc files have exact mappings; only device-tree is a closed inventory.
        bool recursive = !logical || candidate.size() == path.size()
            || !starts_with(candidate, "/proc/") || subtree(candidate, "/proc/device-tree");
        if (recursive) {
            auto found = paths.find(candidate);
            if (found != paths.end()) return &*found;
        }
        size_t slash = candidate.rfind('/');
        if (slash == 0 || slash == std::string_view::npos) break;
        candidate = candidate.substr(0, slash);
    }
    return nullptr;
}

inline std::string redirect_identity_path(const IdentityPathMap &paths, std::string_view logical) {
    const auto *found = identity_path_parent(paths, logical, true);
    if (found == nullptr) return {};
    return found->second + std::string(logical.substr(found->first.size()));
}

inline std::string restore_identity_path(const IdentityPathMap &reverse_paths, const std::string &actual) {
    const auto *found = identity_path_parent(reverse_paths, actual, false);
    return found == nullptr ? actual : found->second + actual.substr(found->first.size());
}

inline IdentityPathMap reverse_identity_paths(const IdentityPathMap &paths) {
    IdentityPathMap result;
    for (const auto &entry : paths) result.emplace(entry.second, entry.first);
    return result;
}

}
