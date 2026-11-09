#include "../../main/cpp/identity_paths.h"
#include <atomic>
#include <cassert>
#include <cstdio>
#include <cstdlib>
#include <new>

static std::atomic<size_t> allocations{0};
void *operator new(size_t size) {
    allocations.fetch_add(1, std::memory_order_relaxed);
    if (void *result = std::malloc(size == 0 ? 1 : size)) return result;
    throw std::bad_alloc();
}
void operator delete(void *value) noexcept { std::free(value); }
void operator delete(void *value, size_t) noexcept { std::free(value); }
void *operator new[](size_t size) { return ::operator new(size); }
void operator delete[](void *value) noexcept { std::free(value); }
void operator delete[](void *value, size_t) noexcept { std::free(value); }

int main() {
    smd::IdentityPathMap paths{
        {"/sys/devices/system/cpu", "/cache/cpu"},
        {"/sys/devices/system/cpu/cpu0/cpufreq", "/cache/policy"},
        {"/proc/version", "/cache/version"},
        {"/proc/device-tree", "/cache/tree"}
    };
    assert(smd::normalize_identity_path("/sys//devices/./system/cpu/") == "/sys/devices/system/cpu");
    assert(smd::normalize_identity_path("/data/../sys/devices/system/cpu") == "/sys/devices/system/cpu");
    assert(smd::normalize_identity_path("/../../") == "/");
    assert(smd::redirect_identity_path(paths, "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq") == "/cache/policy/scaling_cur_freq");
    assert(smd::redirect_identity_path(paths, "/sys/devices/system/cpu/cpu1/unknown") == "/cache/cpu/cpu1/unknown");
    assert(smd::redirect_identity_path(paths, "/proc/version") == "/cache/version");
    assert(smd::redirect_identity_path(paths, "/proc/version/child").empty());
    assert(smd::redirect_identity_path(paths, "/proc/device-tree/memory/dram_type") == "/cache/tree/memory/dram_type");
    assert(smd::redirect_identity_path(paths, "/sys/devices/system/cpufrequency").empty());
    auto reverse = smd::reverse_identity_paths(paths);
    assert(smd::restore_identity_path(reverse, "/cache/policy/scaling_cur_freq") == "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq");
    assert(smd::restore_identity_path(reverse, "/cache/cpu/cpu1/unknown") == "/sys/devices/system/cpu/cpu1/unknown");
    assert(smd::restore_identity_path(reverse, "/cache/cpus/file") == "/cache/cpus/file");

    for (int index = 0; index < 4096; ++index) paths.emplace("/sys/bus/platform/drivers/driver" + std::to_string(index), "/cache/driver" + std::to_string(index));
    const size_t before = allocations.load(std::memory_order_relaxed);
    for (int index = 0; index < 10000; ++index) {
        assert(!smd::may_redirect_identity_path("/data/user/0/example/cache/database.db"));
        assert(!smd::may_redirect_identity_path("/data/app/example/base.apk"));
        assert(!smd::may_redirect_identity_path("/system/fonts/Roboto.ttf"));
        assert(!smd::may_redirect_identity_path("/apex/com.android.art/lib64/libart.so"));
        assert(smd::identity_path_parent(paths, "/sys/power/state", true) == nullptr);
    }
    assert(allocations.load(std::memory_order_relaxed) == before);
    assert(smd::may_redirect_identity_path("cpu0/scaling_cur_freq"));
    assert(smd::may_redirect_identity_path("/data/../sys/devices/system/cpu/cpu0"));
    assert(smd::may_redirect_identity_path("/./sys/devices/system/cpu/cpu0"));
    assert(smd::may_redirect_identity_path("//sys/devices/system/cpu/cpu0"));
    assert(!smd::may_redirect_identity_path(nullptr));
    assert(!smd::may_redirect_identity_path(""));
    assert(!smd::may_redirect_identity_path("/systematic/file"));
    assert(!smd::may_redirect_identity_path("/processor/file"));
    std::puts("Native identity path assertions passed; unrelated paths and missed lookups allocate no memory.");
}
