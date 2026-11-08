package com.devicespooflab.hooks.utils;

import com.bytedance.shadowhook.ShadowHook;
import java.util.Map;
import de.robv.android.xposed.XposedBridge;

/** Native hooks are loaded only inside the eligible, boot-ready application process. */
public final class NativeIdentity {
    private static volatile boolean loaded;
    private static volatile boolean attempted;
    private NativeIdentity() { }

    public static synchronized void install() {
        if (attempted) return;
        attempted = true;
        try {
            int error = ShadowHook.init(new ShadowHook.ConfigBuilder().setMode(ShadowHook.Mode.SHARED).build());
            if (error != 0) throw new IllegalStateException("ShadowHook init: " + error);
            System.loadLibrary("smdidentity");
            loaded = nativeInstall();
            ScopedHardwareRuntime.View view = ScopedHardwareRuntime.current();
            publish(view.properties, view.paths);
            XposedBridge.log("SpoofMyDevice: Native identity hooks " + (loaded ? "ready" : "unavailable"));
        } catch (Throwable error) {
            XposedBridge.log("SpoofMyDevice: Native identity initialization failed: " + error);
        }
    }

    public static void publish(Map<String, String> properties, Map<String, String> paths) {
        if (!loaded) return;
        nativePublish(properties.keySet().toArray(new String[0]), properties.values().toArray(new String[0]),
            paths.keySet().toArray(new String[0]), paths.values().toArray(new String[0]));
    }

    public static void bypassRuntimeVersion(boolean enabled) {
        if (loaded) nativeBypassRuntimeVersion(enabled);
    }

    private static native boolean nativeInstall();
    private static native void nativePublish(String[] keys, String[] values, String[] paths, String[] targets);
    private static native void nativeBypassRuntimeVersion(boolean enabled);
}
