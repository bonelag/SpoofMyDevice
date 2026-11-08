package com.devicespooflab.hooks.hooks;

import com.devicespooflab.hooks.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks java.lang.System.getProperty for values that some apps use as device metadata.
 */
public final class JavaSystemPropertyHooks {

    private JavaSystemPropertyHooks() {
    }

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(System.class, "getProperty",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String key = (String) param.args[0];
                        String spoofed = getSpoofedValue(key);
                        if (spoofed != null) {
                            param.setResult(spoofed);
                        }
                    }
                });
        } catch (Throwable ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(System.class, "getProperty",
                String.class, String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String key = (String) param.args[0];
                        String spoofed = getSpoofedValue(key);
                        if (spoofed != null) {
                            param.setResult(spoofed);
                        }
                    }
                });
        } catch (Throwable ignored) {
        }
    }

    private static String getSpoofedValue(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        if (key.equals("os.arch") || key.equals("os.version") || key.equals("os.name")) {
            return ConfigManager.hardwareIdentity().javaProperty(key);
        }
        if ("http.agent".equals(key)) {
            return ConfigManager.getWebViewUserAgent();
        }
        return null;
    }
}
