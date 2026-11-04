package com.devicespooflab.hooks.utils;

/** Decisions that must not read preferences, start another app, or acquire a Binder service. */
public final class HookSafetyPolicy {

    private static final int PER_USER_RANGE = 100000;
    private static final int FIRST_APPLICATION_UID = 10000;
    private static final int LAST_APPLICATION_UID = 19999;
    private static final int SYSTEM_APP_FLAGS = 1 | 128; // FLAG_SYSTEM | FLAG_UPDATED_SYSTEM_APP

    private HookSafetyPolicy() {
    }

    public static boolean isBootReady(String bootCompleted, String bootAnimationState) {
        // A soft reboot can leave sys.boot_completed=1 while bootanimation runs again.
        return "1".equals(bootCompleted) && "stopped".equals(bootAnimationState);
    }

    public static boolean isProtectedUid(int uid) {
        int appId = uid % PER_USER_RANGE;
        return uid < 0 || appId < FIRST_APPLICATION_UID || appId > LAST_APPLICATION_UID;
    }

    public static boolean isProtectedProcess(
        String packageName,
        String processName,
        int processUid,
        int applicationFlags
    ) {
        return isProtectedUid(processUid)
            || (applicationFlags & SYSTEM_APP_FLAGS) != 0
            || isProtectedPackage(packageName)
            || "zygote".equals(processName)
            || "zygote64".equals(processName)
            || "system_server".equals(processName);
    }

    public static boolean isProtectedPackage(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return true;
        }
        return "android".equals(packageName)
            || "system".equals(packageName)
            || "system_server".equals(packageName)
            || "com.android.systemui".equals(packageName)
            || packageName.startsWith("org.lsposed.")
            || packageName.startsWith("me.weishu.kernelsu")
            || packageName.startsWith("io.github.a13e300.ksu")
            || packageName.startsWith("com.topjohnwu.magisk")
            || packageName.contains(".launcher")
            || packageName.startsWith("com.bbk.launcher")
            || packageName.startsWith("com.vivo.upslide")
            || packageName.startsWith("com.vivo.daemonService")
            || packageName.startsWith("com.vivo.fingerprint")
            || packageName.startsWith("com.vivo.ambient")
            || packageName.startsWith("com.vivo.secime")
            || packageName.contains("inputmethod")
            || packageName.startsWith("com.android.server")
            || packageName.startsWith("com.android.keyguard")
            || packageName.startsWith("com.android.wallpaper");
    }

    public static boolean isRuntimeStateProperty(String key) {
        return key != null && (key.startsWith("init.svc.")
            || key.startsWith("sys.boot")
            || "dev.bootcomplete".equals(key)
            || "service.bootanim.exit".equals(key)
            || "sys.shutdown.requested".equals(key)
            || "sys.powerctl".equals(key));
    }
}
