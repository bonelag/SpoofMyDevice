package com.devicespooflab.hooks.hooks;

import com.devicespooflab.hooks.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class OpenGLHooks {

    private static final String TAG = "SpoofMyDevice-OpenGL";

    private static final int GL_VENDOR = 0x1F00;
    private static final int GL_RENDERER = 0x1F01;
    private static final int GL_VERSION = 0x1F02;
    private static final int GL_SHADING_LANGUAGE_VERSION = 0x8B8C;

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        hookGles20(lpparam);
        hookGles10(lpparam);
        hookGles30(lpparam);
        hookGl10(lpparam);
    }

    private static void hookGles20(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.opengl.GLES20",
                lpparam.classLoader,
                "glGetString",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        int name = (Integer) param.args[0];
                        String spoofed = resolveGlString(name);
                        if (spoofed != null) {
                            param.setResult(spoofed);
                        }
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook GLES20.glGetString: " + t.getMessage());
        }
    }

    private static void hookGles10(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.opengl.GLES10",
                lpparam.classLoader,
                "glGetString",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        int name = (Integer) param.args[0];
                        String spoofed = resolveGlString(name);
                        if (spoofed != null) {
                            param.setResult(spoofed);
                        }
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook GLES10.glGetString: " + t.getMessage());
        }
    }

    private static void hookGles30(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.opengl.GLES30",
                lpparam.classLoader,
                "glGetString",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        int name = (Integer) param.args[0];
                        String spoofed = resolveGlString(name);
                        if (spoofed != null) {
                            param.setResult(spoofed);
                        }
                    }
                }
            );
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook GLES30.glGetString: " + t.getMessage());
        }
    }

    private static void hookGl10(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> glImplClass = XposedHelpers.findClassIfExists("com.google.android.gles_jni.GLImpl", lpparam.classLoader);
            if (glImplClass != null) {
                XposedHelpers.findAndHookMethod(
                    glImplClass,
                    "glGetString",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            int name = (Integer) param.args[0];
                            String spoofed = resolveGlString(name);
                            if (spoofed != null) {
                                param.setResult(spoofed);
                            }
                        }
                    }
                );
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to hook GLImpl.glGetString: " + t.getMessage());
        }
    }

    private static String resolveGlString(int name) {
        if (name == GL_RENDERER) {
            return ConfigManager.getGpuRenderer();
        } else if (name == GL_VENDOR) {
            return ConfigManager.getGpuVendor();
        } else if (name == GL_VERSION) {
            return ConfigManager.getGlVersion();
        } else if (name == GL_SHADING_LANGUAGE_VERSION) {
            return ConfigManager.getGlslVersion();
        }
        return null;
    }
}
