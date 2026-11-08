#include "../../main/cpp/native_hook_guard.h"
#include <cassert>
#include <atomic>
#include <cstdio>
#include <mutex>
#include <stdexcept>
#include <thread>
#include <unistd.h>

std::mutex registry;
int original_reads = 0;
int identity_reads = 0;

int traced_property_read() {
    smd::NativeHookGuard call;
    if (!call.primary()) { ++original_reads; return 7; }
    std::lock_guard<std::mutex> lock(registry);
    // Simulate bionic_trace_end reading a property while this same mutex is held.
    // Without the actual production guard this nested call deadlocks.
    assert(traced_property_read() == 7);
    ++identity_reads;
    return 42;
}

int main() {
    alarm(5);
    assert(traced_property_read() == 42);
    assert(original_reads == 1 && identity_reads == 1);
    {
        smd::NativeHookGuard outer;
        assert(outer.primary());
        { smd::NativeHookGuard recursive; assert(!recursive.primary()); }
        {
            smd::NativeHookGuard::ClientCallback client;
            smd::NativeHookGuard nested_client_read;
            assert(nested_client_read.primary());
        }
        { smd::NativeHookGuard restored; assert(!restored.primary()); }
        std::atomic<bool> other_thread{false};
        std::thread worker([&] { smd::NativeHookGuard independent; other_thread = independent.primary(); });
        worker.join();
        assert(other_thread);
    }
    try { smd::NativeHookGuard call; throw std::runtime_error("callback failure"); }
    catch (const std::runtime_error &) { }
    { smd::NativeHookGuard recovered; assert(recovered.primary()); }
    alarm(0);
    std::puts("PASS: tracing reentry, callback nesting, thread isolation, exception recovery");
}
