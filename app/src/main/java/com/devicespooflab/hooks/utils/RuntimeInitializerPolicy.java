package com.devicespooflab.hooks.utils;

import java.lang.reflect.Modifier;

/** Native hook engines must discover the physical OS before caching private linker layouts. */
public final class RuntimeInitializerPolicy {
    public static final String[] LIBRARY_CLASSES = {
        "com.bytedance.shadowhook.ShadowHook",
        "com.bytedance.android.bytehook.ByteHook"
    };

    private RuntimeInitializerPolicy() { }

    public static boolean isLibraryClass(String name) {
        for (String candidate : LIBRARY_CLASSES) if (candidate.equals(name)) return true;
        return false;
    }

    public static boolean isInitializer(String name, int modifiers, Class<?> result, int parameters) {
        if (!Modifier.isStatic(modifiers) || result != int.class) return false;
        if (Modifier.isNative(modifiers)) return "nativeInit".equals(name);
        // R8 renames the Java init(Config) wrapper, while its synchronized signature remains.
        // Guard the wrapper too: loading the library can discover the SDK before nativeInit.
        return "init".equals(name) || (Modifier.isSynchronized(modifiers) && parameters == 1);
    }
}
