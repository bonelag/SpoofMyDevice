package com.devicespooflab.hooks.utils;

/** Independent ShadowHook copies must not patch the same process linker. */
public final class NativeRuntimeConflictPolicy {
    private static final String SHADOWHOOK_CLASS = "com.bytedance.shadowhook.ShadowHook";

    private NativeRuntimeConflictPolicy() { }

    public static boolean hasHostShadowHook(ClassLoader hostLoader, ClassLoader moduleLoader) {
        if (hostLoader == null || hostLoader == moduleLoader) return false;
        try {
            // Resolve metadata only: probing must not initialize the host's native engine.
            Class<?> runtime = Class.forName(SHADOWHOOK_CLASS, false, hostLoader);
            return runtime.getClassLoader() != moduleLoader;
        } catch (ClassNotFoundException absent) {
            return false;
        } catch (LinkageError incompleteHostRuntime) {
            // A host-owned runtime with unresolved dependencies is not safe to interpose on.
            return true;
        }
    }
}
