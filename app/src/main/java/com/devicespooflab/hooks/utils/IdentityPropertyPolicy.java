package com.devicespooflab.hooks.utils;

/** Driver and ABI values are identity only for application readers, never platform selection code. */
public final class IdentityPropertyPolicy {
    private IdentityPropertyPolicy() { }

    public static boolean sensitive(String key) {
        return key != null && (key.equals("ro.hardware") || key.equals("ro.board.platform")
            || key.equals("ro.hardware.egl") || key.equals("ro.hardware.vulkan") || key.equals("ro.boot.hardware.vulkan")
            || key.equals("ro.hardware.gralloc") || key.equals("ro.hardware.power") || key.startsWith("ro.product.cpu.abi")
            || key.startsWith("ro.mediatek.") || key.startsWith("ro.vendor.mediatek.")
            || key.startsWith("ro.vendor.mtk_") || key.startsWith("ro.mtk."));
    }

    public static boolean preservePlatformCaller(String key, StackTraceElement[] stack) {
        if (!sensitive(key)) return false;
        for (StackTraceElement frame : stack) {
            String name = frame.getClassName();
            if (name.equals("java.lang.Thread") || name.startsWith("android.os.SystemProperties")
                || name.equals("android.os.SemSystemProperties") || name.equals("com.samsung.android.os.SemSystemProperties")
                || name.startsWith("java.lang.reflect.") || name.startsWith("jdk.internal.reflect.")
                || name.startsWith("sun.reflect.") || name.startsWith("com.devicespooflab.hooks.")
                || name.startsWith("de.robv.android.xposed.") || name.startsWith("org.lsposed.")
                || name.startsWith("_LSP_HOOKER_")) continue;
            return name.startsWith("android.") || name.startsWith("com.android.") || name.startsWith("com.samsung.android.")
                || name.startsWith("com.oplus.") || name.startsWith("com.vivo.") || name.startsWith("miui.")
                || name.startsWith("java.")
                || name.startsWith("javax.") || name.startsWith("libcore.") || name.startsWith("dalvik.");
        }
        return true;
    }
}
