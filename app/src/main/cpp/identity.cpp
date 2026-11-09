#include <jni.h>
#include "native_hook_guard.h"
#include "identity_paths.h"
#include <shadowhook.h>
#include <android/sensor.h>
#include <sys/system_properties.h>
#include <sys/utsname.h>
#include <sys/stat.h>
#include <sys/statvfs.h>
#include <sys/vfs.h>
#include <fcntl.h>
#include <unistd.h>
#include <dirent.h>
#include <dlfcn.h>
#include <link.h>
#include <GLES2/gl2.h>
#include <vulkan/vulkan.h>
#include <cstdio>
#include <cstring>
#include <cstdarg>
#include <atomic>
#include <map>
#include <set>
#include <memory>
#include <mutex>
#include <string>
#include <vector>

namespace {
struct State {
    smd::IdentityPathMap properties;
    smd::IdentityPathMap paths;
    smd::IdentityPathMap reverse_paths;
};
std::shared_ptr<const State> current = std::make_shared<const State>();
const std::shared_ptr<const State> inactive = std::make_shared<const State>();
pid_t owner_pid = 0;
std::atomic<bool> runtime_version_bypass{false};
std::mutex strings_mutex;
std::map<std::string, std::unique_ptr<std::string>> permanent_strings;
using ReadCallback = void (*)(const prop_info *, void (*)(void *, const char *, const char *, uint32_t), void *);
ReadCallback original_read_callback = nullptr;
std::set<void *> hooked_addresses;
struct CodeRange {
    std::atomic<uintptr_t> first{0};
    std::atomic<uintptr_t> last{0};
};
CodeRange application_ranges[512];
bool eligible();

bool app_library(const dl_phdr_info *info) {
    return eligible() && info != nullptr && info->dlpi_name != nullptr
        && (std::strstr(info->dlpi_name, "/data/app/") || std::strstr(info->dlpi_name, "/data/user/")
            || std::strstr(info->dlpi_name, "/data/user_de/") || std::strstr(info->dlpi_name, "/data/data/"));
}
void collect_app_code(dl_phdr_info *info, size_t, void *) {
    if (!app_library(info)) return;
    // Linker callbacks use only the supplied ELF headers and a fixed atomic table.
    // No allocation, I/O, linker enumeration or global mutex is permitted here.
    for (size_t index = 0; index < std::min<size_t>(info->dlpi_phnum, 256); index++) {
        const auto &header = info->dlpi_phdr[index];
        if (header.p_type != PT_LOAD || (header.p_flags & PF_X) == 0) continue;
        uintptr_t first = info->dlpi_addr + header.p_vaddr;
        uintptr_t last = first + header.p_memsz;
        bool known = false;
        for (const auto &range : application_ranges) if (range.first.load(std::memory_order_acquire) == first) { known = true; break; }
        if (known) continue;
        for (auto &range : application_ranges) {
            uintptr_t empty = 0;
            if (range.first.compare_exchange_strong(empty, 1)) {
                range.last.store(last, std::memory_order_relaxed);
                range.first.store(first, std::memory_order_release);
                break;
            }
        }
    }
}
void forget_app_code(dl_phdr_info *info, size_t, void *) {
    if (!app_library(info)) return;
    for (size_t index = 0; index < std::min<size_t>(info->dlpi_phnum, 256); index++) {
        const auto &header = info->dlpi_phdr[index];
        if (header.p_type != PT_LOAD || (header.p_flags & PF_X) == 0) continue;
        uintptr_t first = info->dlpi_addr + header.p_vaddr;
        for (auto &range : application_ranges) {
            uintptr_t expected = first;
            if (range.first.compare_exchange_strong(expected, 1)) {
                range.last.store(0, std::memory_order_relaxed);
                range.first.store(0, std::memory_order_release);
            }
        }
    }
}

bool eligible() {
    uid_t app = getuid() % 100000;
    return getpid() == owner_pid && app >= 10000 && app <= 19999;
}
std::shared_ptr<const State> state() {
    // Never acquire shared-pointer/global locks in a forked child before exec.
    if (getpid() != owner_pid) return inactive;
    return std::atomic_load(&current);
}
bool begins(const std::string &text, const char *prefix) { return text.rfind(prefix, 0) == 0; }
bool ends(const std::string &text, const char *suffix) {
    size_t length = std::strlen(suffix);
    return text.size() >= length && text.compare(text.size() - length, length, suffix) == 0;
}
bool runtime_property(const std::string &key) {
    return begins(key, "init.svc.") || begins(key, "sys.boot") || key == "dev.bootcomplete"
        || key == "service.bootanim.exit" || key == "sys.shutdown.requested" || key == "sys.powerctl";
}
const std::string *get(const State &snapshot, std::string_view key) {
    auto item = snapshot.properties.find(key);
    return item == snapshot.properties.end() ? nullptr : &item->second;
}
bool sensitive_driver_key(const std::string &key) {
    return key == "ro.hardware.egl" || key == "ro.hardware.vulkan" || key == "ro.boot.hardware.vulkan" || key == "ro.hardware.gralloc"
        || key == "ro.hardware.power" || key == "ro.board.platform" || key == "ro.hardware"
        || begins(key, "ro.product.cpu.abi") || begins(key, "ro.mediatek.")
        || begins(key, "ro.vendor.mediatek.") || begins(key, "ro.vendor.mtk_") || begins(key, "ro.mtk.");
}
bool application_caller(const State &, void *address) {
    uintptr_t caller = reinterpret_cast<uintptr_t>(address);
    for (const auto &range : application_ranges) {
        uintptr_t first = range.first.load(std::memory_order_acquire);
        if (first > 1 && caller >= first && caller < range.last.load(std::memory_order_relaxed)) return true;
    }
    return false;
}
bool enabled(const State &snapshot, const std::string &field) {
    const auto *value = get(snapshot, "spoof.enabled." + field);
    return value == nullptr || *value == "true" || *value == "1";
}
const std::string *property(const State &snapshot, const std::string &key) {
    if (snapshot.properties.empty() || runtime_property(key)) return nullptr;
    if (runtime_version_bypass.load(std::memory_order_relaxed) && key.find(".build.version.") != std::string::npos) return nullptr;
    const auto *soc_active = get(snapshot, "identity.soc.enabled");
    const auto *soc_vendor = get(snapshot, "identity.soc.manufacturer");
    if (soc_active != nullptr && *soc_active == "true" && soc_vendor != nullptr
        && soc_vendor->find("MediaTek") == std::string::npos && soc_vendor->find("Mediatek") == std::string::npos
        && soc_vendor->find("mediatek") == std::string::npos && soc_vendor->find("MTK") == std::string::npos
        && (begins(key, "ro.mediatek.") || begins(key, "ro.vendor.mediatek.") || begins(key, "ro.vendor.mtk_") || begins(key, "ro.mtk."))) {
        static const std::string empty;
        return &empty;
    }
    if (const auto *direct = get(snapshot, key)) return direct;
    if (key.find("marketname") != std::string::npos || key.find("market.name") != std::string::npos
        || key.find("market_name") != std::string::npos || key.find("marketing_name") != std::string::npos
        || key == "ro.product.nickname" || key == "persist.sys.devicename" || key == "ro.config.devicename") {
        return enabled(snapshot, "model") ? get(snapshot, "ro.product.model") : nullptr;
    }
    if (begins(key, "ro.product.") || begins(key, "ro.vendor.product.")) {
        for (const char *field : {"brand", "manufacturer", "model", "name", "device", "board"}) {
            std::string suffix = "." + std::string(field);
            if (ends(key, suffix.c_str()) && enabled(snapshot, std::strcmp(field, "name") == 0 ? "product" : field)) {
                return get(snapshot, "ro.product." + std::string(field));
            }
        }
    }
    size_t build = key.find(".build.");
    if (build != std::string::npos) return get(snapshot, "ro.build." + key.substr(build + 7));
    if (begins(key, "ro.vendor.soc.model")) return enabled(snapshot, "soc_model") ? get(snapshot, "ro.soc.model") : nullptr;
    if (key == "ro.vendor.soc.manufacturer") return enabled(snapshot, "soc_manufacturer") ? get(snapshot, "ro.soc.manufacturer") : nullptr;
    const auto *brand = get(snapshot, "ro.product.brand");
    if (brand != nullptr && *brand != "vivo" && *brand != "iqoo"
        && (begins(key, "ro.vivo.") || begins(key, "ro.vendor.vivo.") || begins(key, "persist.vivo.")
            || begins(key, "vendor.gsm.project.") || ends(key, ".bbk"))) {
        static const std::string empty;
        return &empty;
    }
    return nullptr;
}
const char *permanent(const std::string &text) {
    std::lock_guard<std::mutex> lock(strings_mutex);
    auto found = permanent_strings.find(text);
    if (found == permanent_strings.end()) found = permanent_strings.emplace(text, std::make_unique<std::string>(text)).first;
    return found->second->c_str();
}
std::string logical_path(const State &snapshot, const char *path, int directory = AT_FDCWD) {
    if (path == nullptr) return {};
    std::string actual(path);
    if (actual.empty()) return {};
    if (actual[0] != '/') {
        char base[4096]{};
        if (directory == AT_FDCWD) {
            if (getcwd(base, sizeof(base)) == nullptr) return {};
        } else {
            char descriptor[64];
            std::snprintf(descriptor, sizeof(descriptor), "/proc/self/fd/%d", directory);
            ssize_t count = readlink(descriptor, base, sizeof(base) - 1);
            if (count < 0) return {};
            base[count] = 0;
        }
        actual = std::string(base) + "/" + actual;
    }
    if (!smd::normalized_identity_path(actual)) actual = smd::normalize_identity_path(actual);
    // openat on an already virtualized directory still uses its original logical path.
    return smd::restore_identity_path(snapshot.reverse_paths, actual);
}
std::string redirect(const char *path, int directory = AT_FDCWD) {
    if (!smd::may_redirect_identity_path(path) || !eligible()) return {};
    auto snapshot = state();
    if (snapshot->paths.empty()) return {};
    return smd::redirect_identity_path(snapshot->paths, logical_path(*snapshot, path, directory));
}
bool read_only(int flags) { return (flags & O_ACCMODE) == O_RDONLY && (flags & (O_CREAT | O_TRUNC)) == 0 && (flags & O_TMPFILE) != O_TMPFILE; }

int proxy_property_get(const char *key, char *output) {
    smd::NativeHookGuard call;
    SHADOWHOOK_STACK_SCOPE();
    if (!call.primary()) return SHADOWHOOK_CALL_PREV(proxy_property_get, key, output);
    auto snapshot = state();
    if (key != nullptr && sensitive_driver_key(key) && !application_caller(*snapshot, SHADOWHOOK_RETURN_ADDRESS())) {
        return SHADOWHOOK_CALL_PREV(proxy_property_get, key, output);
    }
    const auto *value = eligible() && key != nullptr ? property(*snapshot, key) : nullptr;
    if (value == nullptr || output == nullptr) return SHADOWHOOK_CALL_PREV(proxy_property_get, key, output);
    size_t count = std::min(value->size(), static_cast<size_t>(PROP_VALUE_MAX - 1));
    std::memcpy(output, value->data(), count); output[count] = 0;
    return static_cast<int>(count);
}
struct Callback {
    void (*callback)(void *, const char *, const char *, uint32_t);
    void *cookie;
    std::shared_ptr<const State> snapshot;
    void *caller;
};
void property_callback(void *cookie, const char *name, const char *value, uint32_t serial) {
    auto *context = static_cast<Callback *>(cookie);
    const auto *replacement = name != nullptr && (!sensitive_driver_key(name)
        || application_caller(*context->snapshot, context->caller))
        ? property(*context->snapshot, name) : nullptr;
    smd::NativeHookGuard::ClientCallback client;
    // The serial belongs to bionic's actual prop_info buffer and seqlock protocol.
    // Replacing it with our snapshot version can make native readers retry forever.
    context->callback(context->cookie, name, replacement == nullptr ? value : replacement->c_str(), serial);
}
void proxy_property_read_callback(const prop_info *info, void (*callback)(void *, const char *, const char *, uint32_t), void *cookie) {
    smd::NativeHookGuard call;
    SHADOWHOOK_STACK_SCOPE();
    if (!call.primary()) return SHADOWHOOK_CALL_PREV(proxy_property_read_callback, info, callback, cookie);
    if (!eligible() || callback == nullptr) { SHADOWHOOK_CALL_PREV(proxy_property_read_callback, info, callback, cookie); return; }
    auto snapshot = state();
    Callback context{callback, cookie, snapshot, SHADOWHOOK_RETURN_ADDRESS()};
    SHADOWHOOK_CALL_PREV(proxy_property_read_callback, info, property_callback, &context);
}
void capture_property_name(void *cookie, const char *name, const char *, uint32_t) {
    if (name != nullptr) *static_cast<std::string *>(cookie) = name;
}
int proxy_property_read(const prop_info *info, char *name, char *output) {
    smd::NativeHookGuard call;
    SHADOWHOOK_STACK_SCOPE();
    if (!call.primary()) return SHADOWHOOK_CALL_PREV(proxy_property_read, info, name, output);
    if (!eligible()) return SHADOWHOOK_CALL_PREV(proxy_property_read, info, name, output);
    std::string key;
    int count = SHADOWHOOK_CALL_PREV(proxy_property_read, info, name, output);
    if (name != nullptr) key = name;
    else if (original_read_callback != nullptr) original_read_callback(info, capture_property_name, &key);
    auto snapshot = state();
    if (sensitive_driver_key(key) && !application_caller(*snapshot, SHADOWHOOK_RETURN_ADDRESS())) {
        return count;
    }
    const auto *value = property(*snapshot, key);
    if (value != nullptr && output != nullptr) {
        size_t length = std::min(value->size(), static_cast<size_t>(PROP_VALUE_MAX - 1));
        std::memcpy(output, value->data(), length); output[length] = 0;
        return static_cast<int>(length);
    }
    return count;
}

int proxy_open(const char *path, int flags, ...) {
    smd::NativeHookGuard call;
    SHADOWHOOK_STACK_SCOPE();
    mode_t mode = 0;
    if ((flags & O_CREAT) != 0 || (flags & O_TMPFILE) == O_TMPFILE) { va_list arguments; va_start(arguments, flags); mode = va_arg(arguments, int); va_end(arguments); }
    if (!call.primary()) return SHADOWHOOK_CALL_PREV(proxy_open, path, flags, mode);
    std::string target = read_only(flags) ? redirect(path) : std::string();
    return SHADOWHOOK_CALL_PREV(proxy_open, target.empty() ? path : target.c_str(), flags, mode);
}
int proxy_openat(int directory, const char *path, int flags, ...) {
    smd::NativeHookGuard call;
    SHADOWHOOK_STACK_SCOPE();
    mode_t mode = 0;
    if ((flags & O_CREAT) != 0 || (flags & O_TMPFILE) == O_TMPFILE) { va_list arguments; va_start(arguments, flags); mode = va_arg(arguments, int); va_end(arguments); }
    if (!call.primary()) return SHADOWHOOK_CALL_PREV(proxy_openat, directory, path, flags, mode);
    std::string target = read_only(flags) ? redirect(path, directory) : std::string();
    return SHADOWHOOK_CALL_PREV(proxy_openat, target.empty() ? directory : AT_FDCWD, target.empty() ? path : target.c_str(), flags, mode);
}
int proxy_open_2(const char *path, int flags) {
    smd::NativeHookGuard call;
    SHADOWHOOK_STACK_SCOPE();
    if (!call.primary()) return SHADOWHOOK_CALL_PREV(proxy_open_2, path, flags);
    std::string target = read_only(flags) ? redirect(path) : std::string();
    return SHADOWHOOK_CALL_PREV(proxy_open_2, target.empty() ? path : target.c_str(), flags);
}
int proxy_openat_2(int directory, const char *path, int flags) {
    smd::NativeHookGuard call;
    SHADOWHOOK_STACK_SCOPE();
    if (!call.primary()) return SHADOWHOOK_CALL_PREV(proxy_openat_2, directory, path, flags);
    std::string target = read_only(flags) ? redirect(path, directory) : std::string();
    return SHADOWHOOK_CALL_PREV(proxy_openat_2, target.empty() ? directory : AT_FDCWD, target.empty() ? path : target.c_str(), flags);
}
FILE *proxy_fopen(const char *path, const char *mode) {
    smd::NativeHookGuard call;
    SHADOWHOOK_STACK_SCOPE();
    if (!call.primary()) return SHADOWHOOK_CALL_PREV(proxy_fopen, path, mode);
    std::string target = mode != nullptr && mode[0] == 'r' && std::strchr(mode, '+') == nullptr ? redirect(path) : std::string();
    return SHADOWHOOK_CALL_PREV(proxy_fopen, target.empty() ? path : target.c_str(), mode);
}
int proxy_stat(const char *path, struct stat *data) {
    smd::NativeHookGuard call;
    SHADOWHOOK_STACK_SCOPE();
    if (!call.primary()) return SHADOWHOOK_CALL_PREV(proxy_stat, path, data);
    std::string target = redirect(path);
    return SHADOWHOOK_CALL_PREV(proxy_stat, target.empty() ? path : target.c_str(), data);
}
int proxy_lstat(const char *path, struct stat *data) {
    smd::NativeHookGuard call;
    SHADOWHOOK_STACK_SCOPE();
    if (!call.primary()) return SHADOWHOOK_CALL_PREV(proxy_lstat, path, data);
    std::string target = redirect(path);
    return SHADOWHOOK_CALL_PREV(proxy_lstat, target.empty() ? path : target.c_str(), data);
}
int proxy_stat64(const char *path, struct stat64 *data) {
    smd::NativeHookGuard call;
    SHADOWHOOK_STACK_SCOPE();
    if (!call.primary()) return SHADOWHOOK_CALL_PREV(proxy_stat64, path, data);
    std::string target = redirect(path);
    return SHADOWHOOK_CALL_PREV(proxy_stat64, target.empty() ? path : target.c_str(), data);
}
int proxy_lstat64(const char *path, struct stat64 *data) {
    smd::NativeHookGuard call;
    SHADOWHOOK_STACK_SCOPE();
    if (!call.primary()) return SHADOWHOOK_CALL_PREV(proxy_lstat64, path, data);
    std::string target = redirect(path);
    return SHADOWHOOK_CALL_PREV(proxy_lstat64, target.empty() ? path : target.c_str(), data);
}
int proxy_fstatat(int directory, const char *path, struct stat *data, int flags) {
    smd::NativeHookGuard call;
    SHADOWHOOK_STACK_SCOPE();
    if (!call.primary()) return SHADOWHOOK_CALL_PREV(proxy_fstatat, directory, path, data, flags);
    std::string target = redirect(path, directory);
    return SHADOWHOOK_CALL_PREV(proxy_fstatat, target.empty() ? directory : AT_FDCWD, target.empty() ? path : target.c_str(), data, flags);
}
int proxy_fstatat64(int directory, const char *path, struct stat64 *data, int flags) {
    smd::NativeHookGuard call;
    SHADOWHOOK_STACK_SCOPE();
    if (!call.primary()) return SHADOWHOOK_CALL_PREV(proxy_fstatat64, directory, path, data, flags);
    std::string target = redirect(path, directory);
    return SHADOWHOOK_CALL_PREV(proxy_fstatat64, target.empty() ? directory : AT_FDCWD, target.empty() ? path : target.c_str(), data, flags);
}
ssize_t proxy_readlink(const char *path, char *output, size_t size) {
    smd::NativeHookGuard call;
    SHADOWHOOK_STACK_SCOPE();
    if (!call.primary()) return SHADOWHOOK_CALL_PREV(proxy_readlink, path, output, size);
    std::string target = redirect(path);
    return SHADOWHOOK_CALL_PREV(proxy_readlink, target.empty() ? path : target.c_str(), output, size);
}
ssize_t proxy_readlinkat(int directory, const char *path, char *output, size_t size) {
    smd::NativeHookGuard call;
    SHADOWHOOK_STACK_SCOPE();
    if (!call.primary()) return SHADOWHOOK_CALL_PREV(proxy_readlinkat, directory, path, output, size);
    std::string target = redirect(path, directory);
    return SHADOWHOOK_CALL_PREV(proxy_readlinkat, target.empty() ? directory : AT_FDCWD, target.empty() ? path : target.c_str(), output, size);
}
int proxy_access(const char *path, int mode) {
    smd::NativeHookGuard call;
    SHADOWHOOK_STACK_SCOPE();
    if (!call.primary()) return SHADOWHOOK_CALL_PREV(proxy_access, path, mode);
    std::string target = (mode & W_OK) == 0 ? redirect(path) : std::string();
    return SHADOWHOOK_CALL_PREV(proxy_access, target.empty() ? path : target.c_str(), mode);
}
DIR *proxy_opendir(const char *path) {
    smd::NativeHookGuard call;
    SHADOWHOOK_STACK_SCOPE();
    if (!call.primary()) return SHADOWHOOK_CALL_PREV(proxy_opendir, path);
    std::string target = redirect(path);
    return SHADOWHOOK_CALL_PREV(proxy_opendir, target.empty() ? path : target.c_str());
}
void copy(char *target, size_t size, const std::string *value) {
    if (value == nullptr || size == 0) return;
    std::strncpy(target, value->c_str(), size - 1); target[size - 1] = 0;
}
int proxy_uname(struct utsname *data) {
    smd::NativeHookGuard call;
    SHADOWHOOK_STACK_SCOPE();
    if (!call.primary()) return SHADOWHOOK_CALL_PREV(proxy_uname, data);
    int result = SHADOWHOOK_CALL_PREV(proxy_uname, data);
    auto snapshot = state();
    const auto *active = get(*snapshot, "identity.kernel.enabled");
    if (result == 0 && data != nullptr && eligible() && active != nullptr && *active == "true") {
        std::strncpy(data->sysname, "Linux", sizeof(data->sysname));
        copy(data->nodename, sizeof(data->nodename), get(*snapshot, "identity.kernel.hostname"));
        copy(data->release, sizeof(data->release), get(*snapshot, "identity.kernel.release"));
        copy(data->version, sizeof(data->version), get(*snapshot, "identity.kernel.version"));
        copy(data->machine, sizeof(data->machine), get(*snapshot, "identity.kernel.machine"));
    }
    return result;
}
const char *proxy_sensor_name(const ASensor *sensor) {
    smd::NativeHookGuard call;
    SHADOWHOOK_STACK_SCOPE();
    if (!call.primary()) return SHADOWHOOK_CALL_PREV(proxy_sensor_name, sensor);
    auto snapshot = state(); const auto *active = get(*snapshot, "identity.sensor.enabled");
    if (eligible() && active != nullptr && *active == "true") {
        const auto *value = get(*snapshot, "identity.sensor." + std::to_string(ASensor_getType(sensor)) + ".name");
        if (value != nullptr) return permanent(*value);
    }
    return SHADOWHOOK_CALL_PREV(proxy_sensor_name, sensor);
}
const char *proxy_sensor_vendor(const ASensor *sensor) {
    smd::NativeHookGuard call;
    SHADOWHOOK_STACK_SCOPE();
    if (!call.primary()) return SHADOWHOOK_CALL_PREV(proxy_sensor_vendor, sensor);
    auto snapshot = state(); const auto *active = get(*snapshot, "identity.sensor.enabled");
    if (eligible() && active != nullptr && *active == "true") {
        const auto *value = get(*snapshot, "identity.sensor." + std::to_string(ASensor_getType(sensor)) + ".vendor");
        if (value != nullptr) return permanent(*value);
    }
    return SHADOWHOOK_CALL_PREV(proxy_sensor_vendor, sensor);
}
const GLubyte *proxy_gl_get_string(GLenum name) {
    smd::NativeHookGuard call;
    SHADOWHOOK_STACK_SCOPE();
    if (!call.primary()) return SHADOWHOOK_CALL_PREV(proxy_gl_get_string, name);
    auto snapshot = state(); const std::string *value = nullptr;
    if (eligible()) {
        if (name == GL_VENDOR && enabled(*snapshot, "gpu_vendor")) value = get(*snapshot, "gpu.vendor");
        if (name == GL_RENDERER && enabled(*snapshot, "gpu_renderer")) value = get(*snapshot, "gpu.renderer");
        if (name == GL_VERSION && enabled(*snapshot, "gl_version")) value = get(*snapshot, "gl.version");
        if (name == GL_SHADING_LANGUAGE_VERSION && enabled(*snapshot, "glsl_version")) value = get(*snapshot, "gl.sl_version");
    }
    return value == nullptr ? SHADOWHOOK_CALL_PREV(proxy_gl_get_string, name) : reinterpret_cast<const GLubyte *>(permanent(*value));
}
void patch_vulkan(VkPhysicalDeviceProperties *properties, const State &snapshot) {
    if (properties == nullptr || !eligible() || snapshot.properties.empty()) return;
    if (enabled(snapshot, "gpu_renderer")) copy(properties->deviceName, sizeof(properties->deviceName), get(snapshot, "gpu.renderer"));
    const auto *vendor = get(snapshot, "gpu.vendor");
    if (vendor != nullptr && enabled(snapshot, "gpu_vendor")) {
        if (vendor->find("Imagination") != std::string::npos) properties->vendorID = 0x1010;
        else if (vendor->find("Qualcomm") != std::string::npos) properties->vendorID = 0x5143;
        else if (vendor->find("ARM") != std::string::npos) properties->vendorID = 0x13b5;
    }
}
void proxy_vk_properties(VkPhysicalDevice device, VkPhysicalDeviceProperties *properties) {
    smd::NativeHookGuard call;
    SHADOWHOOK_STACK_SCOPE();
    if (!call.primary()) return SHADOWHOOK_CALL_PREV(proxy_vk_properties, device, properties);
    SHADOWHOOK_CALL_PREV(proxy_vk_properties, device, properties);
    auto snapshot = state(); patch_vulkan(properties, *snapshot);
}
void proxy_vk_properties2(VkPhysicalDevice device, VkPhysicalDeviceProperties2 *properties) {
    smd::NativeHookGuard call;
    SHADOWHOOK_STACK_SCOPE();
    if (!call.primary()) return SHADOWHOOK_CALL_PREV(proxy_vk_properties2, device, properties);
    SHADOWHOOK_CALL_PREV(proxy_vk_properties2, device, properties);
    auto snapshot = state(); if (properties != nullptr) patch_vulkan(&properties->properties, *snapshot);
}

void *hook(const char *library, const char *name, void *replacement, void **original = nullptr) {
    // Bionic exposes multiple names at one address (open/open64, stat/stat64).
    // Register one proxy per address so shared-mode aliases do not create duplicate chains.
    void *handle = shadowhook_dlopen(library);
    void *address = handle == nullptr ? nullptr : shadowhook_dlsym(handle, name);
    if (handle != nullptr) shadowhook_dlclose(handle);
    if (address != nullptr && hooked_addresses.count(address) != 0) return address;
    void *result = shadowhook_hook_sym_name(library, name, replacement, original);
    if (result != nullptr && address != nullptr) hooked_addresses.insert(address);
    return result;
}
void map(JNIEnv *environment, jobjectArray keys, jobjectArray values, smd::IdentityPathMap &target) {
    if (keys == nullptr || values == nullptr) return;
    jsize count = std::min(environment->GetArrayLength(keys), environment->GetArrayLength(values));
    for (jsize index = 0; index < count; index++) {
        auto key = static_cast<jstring>(environment->GetObjectArrayElement(keys, index));
        auto value = static_cast<jstring>(environment->GetObjectArrayElement(values, index));
        if (key != nullptr && value != nullptr) {
            const char *key_bytes = environment->GetStringUTFChars(key, nullptr);
            const char *value_bytes = environment->GetStringUTFChars(value, nullptr);
            if (key_bytes != nullptr && value_bytes != nullptr) target.emplace(key_bytes, value_bytes);
            if (key_bytes != nullptr) environment->ReleaseStringUTFChars(key, key_bytes);
            if (value_bytes != nullptr) environment->ReleaseStringUTFChars(value, value_bytes);
        }
        environment->DeleteLocalRef(key); environment->DeleteLocalRef(value);
    }
}
int code_ranges(dl_phdr_info *information, size_t, void *cookie) {
    collect_app_code(information, 0, cookie);
    return 0;
}
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_devicespooflab_hooks_utils_NativeIdentity_nativeInstall(JNIEnv *, jclass) {
    smd::NativeHookGuard call;
    if (!call.primary()) return JNI_FALSE;
    owner_pid = getpid();
    uid_t app = getuid() % 100000;
    if (app < 10000 || app > 19999) return JNI_FALSE;
    shadowhook_register_dl_init_callback(collect_app_code, nullptr, nullptr);
    shadowhook_register_dl_fini_callback(forget_app_code, nullptr, nullptr);
    bool success = hook("libc.so", "__system_property_get", reinterpret_cast<void *>(proxy_property_get)) != nullptr;
    hook("libc.so", "__system_property_read_callback", reinterpret_cast<void *>(proxy_property_read_callback),
        reinterpret_cast<void **>(&original_read_callback));
    hook("libc.so", "__system_property_read", reinterpret_cast<void *>(proxy_property_read));
    // Never fabricate opaque prop_info handles or hook bionic's serial/seqlock.
    // Missing aliases remain available through property_get and Java/shell hooks.
    hook("libc.so", "open", reinterpret_cast<void *>(proxy_open));
    hook("libc.so", "open64", reinterpret_cast<void *>(proxy_open));
    hook("libc.so", "openat", reinterpret_cast<void *>(proxy_openat));
    hook("libc.so", "openat64", reinterpret_cast<void *>(proxy_openat));
    hook("libc.so", "__open_2", reinterpret_cast<void *>(proxy_open_2));
    hook("libc.so", "__openat_2", reinterpret_cast<void *>(proxy_openat_2));
    hook("libc.so", "fopen", reinterpret_cast<void *>(proxy_fopen));
    hook("libc.so", "fopen64", reinterpret_cast<void *>(proxy_fopen));
    hook("libc.so", "stat", reinterpret_cast<void *>(proxy_stat));
    hook("libc.so", "stat64", reinterpret_cast<void *>(proxy_stat64));
    hook("libc.so", "lstat", reinterpret_cast<void *>(proxy_lstat));
    hook("libc.so", "lstat64", reinterpret_cast<void *>(proxy_lstat64));
    hook("libc.so", "fstatat", reinterpret_cast<void *>(proxy_fstatat));
    hook("libc.so", "fstatat64", reinterpret_cast<void *>(proxy_fstatat64));
    hook("libc.so", "readlink", reinterpret_cast<void *>(proxy_readlink));
    hook("libc.so", "readlinkat", reinterpret_cast<void *>(proxy_readlinkat));
    hook("libc.so", "access", reinterpret_cast<void *>(proxy_access));
    hook("libc.so", "opendir", reinterpret_cast<void *>(proxy_opendir));
    hook("libc.so", "uname", reinterpret_cast<void *>(proxy_uname));
    hook("libandroid.so", "ASensor_getName", reinterpret_cast<void *>(proxy_sensor_name));
    hook("libandroid.so", "ASensor_getVendor", reinterpret_cast<void *>(proxy_sensor_vendor));
    hook("libGLESv2.so", "glGetString", reinterpret_cast<void *>(proxy_gl_get_string));
    hook("libvulkan.so", "vkGetPhysicalDeviceProperties", reinterpret_cast<void *>(proxy_vk_properties));
    hook("libvulkan.so", "vkGetPhysicalDeviceProperties2", reinterpret_cast<void *>(proxy_vk_properties2));
    return success ? JNI_TRUE : JNI_FALSE;
}
extern "C" JNIEXPORT void JNICALL Java_com_devicespooflab_hooks_utils_NativeIdentity_nativePublish(JNIEnv *environment, jclass,
    jobjectArray keys, jobjectArray values, jobjectArray paths, jobjectArray targets) {
    smd::NativeHookGuard call;
    if (!call.primary()) return;
    auto snapshot = std::make_shared<State>();
    map(environment, keys, values, snapshot->properties);
    map(environment, paths, targets, snapshot->paths);
    snapshot->reverse_paths = smd::reverse_identity_paths(snapshot->paths);
    // Enumerate on the configuration worker, never while a linker callback is running.
    dl_iterate_phdr(code_ranges, snapshot.get());
    std::shared_ptr<const State> immutable = snapshot;
    std::atomic_store(&current, immutable);
}

extern "C" JNIEXPORT void JNICALL Java_com_devicespooflab_hooks_utils_NativeIdentity_nativeBypassRuntimeVersion(JNIEnv *, jclass, jboolean enabled) {
    runtime_version_bypass.store(enabled == JNI_TRUE, std::memory_order_relaxed);
}
