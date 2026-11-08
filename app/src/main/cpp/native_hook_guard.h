#pragma once

namespace smd {

// libc mutex tracing can call property/file APIs while an identity hook holds a
// C++ mutex. Recursive calls must use bionic directly, without reading hook state.
class NativeHookGuard {
public:
    NativeHookGuard() noexcept : primary_(!active()) { if (primary_) active() = true; }
    ~NativeHookGuard() { if (primary_) active() = false; }
    NativeHookGuard(const NativeHookGuard &) = delete;
    NativeHookGuard &operator=(const NativeHookGuard &) = delete;
    bool primary() const noexcept { return primary_; }

    // The original property reader has finished before it invokes the client.
    // Client callbacks may legitimately perform another spoofed identity read.
    class ClientCallback {
    public:
        ClientCallback() noexcept : saved_(active()) { active() = false; }
        ~ClientCallback() { active() = saved_; }
        ClientCallback(const ClientCallback &) = delete;
        ClientCallback &operator=(const ClientCallback &) = delete;
    private:
        bool saved_;
    };

private:
    static bool &active() noexcept { static thread_local bool value = false; return value; }
    bool primary_;
};

}
