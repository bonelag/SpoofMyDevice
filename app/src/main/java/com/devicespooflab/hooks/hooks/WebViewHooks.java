package com.devicespooflab.hooks.hooks;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.devicespooflab.hooks.utils.ConfigManager;
import com.devicespooflab.hooks.utils.RuntimeInitializerPolicy;
import com.devicespooflab.hooks.utils.RuntimeSdkScope;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Applies the active profile's User-Agent. Runtime API guards are installed only
 * when its effective SDK differs from the OS, so normal UI dispatch stays unhooked.
 */
public class WebViewHooks {

    private static final String TAG = "DeviceSpoofLab-WebView";
    private static final Set<Class<?>> SDK_RUNTIME_CLASSES = ConcurrentHashMap.newKeySet();
    private static final Set<Class<?>> NATIVE_RUNTIME_CLASSES = ConcurrentHashMap.newKeySet();
    private static final Set<ClassLoader> SDK_RUNTIME_LOADERS = ConcurrentHashMap.newKeySet();
    private static final Set<Class<?>> USER_AGENT_CLASSES = ConcurrentHashMap.newKeySet();
    private static boolean runtimeSdkHooksInstalled;
    private static volatile boolean runtimeSdkGuardNeeded;

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            refreshRuntimeSdkGuard(lpparam);
            hookWebSettings(lpparam);
            hookDefaultUserAgent(lpparam);
            hookWebViewConstructor(lpparam);
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook WebView: " + e.getMessage());
        }
    }

    /** Invoked on the main thread before initial or reloaded Build fields are applied. */
    public static void refreshRuntimeSdkGuard(XC_LoadPackage.LoadPackageParam lpparam) {
        runtimeSdkGuardNeeded = BuildHooks.requiresRuntimeSdkGuard(lpparam.packageName);
        if (!runtimeSdkGuardNeeded) return;
        hookNativeRuntimeInitializers(lpparam.classLoader);
        if (runtimeSdkHooksInstalled) return;
        hookRuntimeSdkReaders();
        hookRuntimeDispatch(lpparam.classLoader);
        runtimeSdkHooksInstalled = true;
    }

    private static boolean runtimeInfoClass(String name) {
        return name.equals("org.chromium.base.AndroidInfo") || name.equals("org.chromium.base.BuildInfo");
    }

    private static XC_MethodHook runtimeSdkHook() {
        return new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                if (runtimeSdkGuardNeeded) param.setObjectExtra("smd.runtimeSdk", BuildHooks.useRuntimeSdk());
            }
            @Override protected void afterHookedMethod(MethodHookParam param) {
                Object lease = param.getObjectExtra("smd.runtimeSdk");
                if (lease instanceof RuntimeSdkScope.Lease) ((RuntimeSdkScope.Lease) lease).close();
            }
        };
    }

    private static void installRuntimeSdkReaders(Class<?> type) {
        if (!SDK_RUNTIME_CLASSES.add(type)) return;
        if (type.getClassLoader() != null) SDK_RUNTIME_LOADERS.add(type.getClassLoader());
        XC_MethodHook guard = runtimeSdkHook();
        for (Method method : type.getDeclaredMethods()) XposedBridge.hookMethod(method, guard);
        XposedBridge.hookAllConstructors(type, guard);
        XposedBridge.log(TAG + ": Runtime SDK guard installed for " + type.getName());
    }

    private static void hookNativeRuntimeInitializers(ClassLoader loader) {
        for (String name : RuntimeInitializerPolicy.LIBRARY_CLASSES) {
            try {
                Class<?> type = XposedHelpers.findClassIfExists(name, loader);
                if (type != null) installNativeRuntimeInitializers(type);
            } catch (Throwable unavailable) {
                XposedBridge.log(TAG + ": Native initializer unavailable: " + name);
            }
        }
    }

    private static void installNativeRuntimeInitializers(Class<?> type) {
        if (!NATIVE_RUNTIME_CLASSES.add(type)) return;
        try {
            boolean installed = false;
            for (Method method : type.getDeclaredMethods()) {
                if (RuntimeInitializerPolicy.isInitializer(method.getName(), method.getModifiers(),
                    method.getReturnType(), method.getParameterTypes().length)) {
                    XposedBridge.hookMethod(method, runtimeSdkHook());
                    installed = true;
                }
            }
            if (installed) XposedBridge.log(TAG + ": Native initializer SDK guard installed for " + type.getName());
        } catch (Throwable unavailable) {
            NATIVE_RUNTIME_CLASSES.remove(type);
            XposedBridge.log(TAG + ": Native initializer SDK guard failed: " + type.getName());
        }
        // App native libraries share the app's loader. Do not classify its Handler callbacks
        // as Chromium callbacks, or ordinary app dispatch would also use the physical SDK.
    }

    private static boolean webViewRuntime(Object value) {
        if (value == null) return false;
        Class<?> type = value.getClass();
        String name = type.getName();
        ClassLoader loader = type.getClassLoader();
        return name.startsWith("org.chromium.") || name.startsWith("com.android.webview.chromium.")
            || name.startsWith("WV.") || (loader != null && SDK_RUNTIME_LOADERS.contains(loader));
    }

    private static void hookRuntimeDispatch(ClassLoader loader) {
        Class<?> webView = XposedHelpers.findClassIfExists("android.webkit.WebView", loader);
        if (webView != null) {
            XC_MethodHook guard = runtimeSdkHook();
            XposedBridge.hookAllConstructors(webView, guard);
            for (Method method : webView.getDeclaredMethods()) {
                try { XposedBridge.hookMethod(method, guard); }
                catch (Throwable unavailable) { }
            }
        }
        Class<?> factory = XposedHelpers.findClassIfExists("android.webkit.WebViewFactory", loader);
        if (factory != null) XposedBridge.hookAllMethods(factory, "getProvider", runtimeSdkHook());
        Class<?> settings = XposedHelpers.findClassIfExists("android.webkit.WebSettings", loader);
        if (settings != null) XposedBridge.hookAllMethods(settings, "getDefaultUserAgent", runtimeSdkHook());
        XposedHelpers.findAndHookMethod(Handler.class, "dispatchMessage", Message.class, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                if (!runtimeSdkGuardNeeded) return;
                Message message = (Message) param.args[0];
                if (webViewRuntime(message.getCallback()) || webViewRuntime(param.thisObject)
                    || webViewRuntime(XposedHelpers.getObjectField(param.thisObject, "mCallback"))) {
                    param.setObjectExtra("smd.runtimeSdk", BuildHooks.useRuntimeSdk());
                }
            }
            @Override protected void afterHookedMethod(MethodHookParam param) {
                Object lease = param.getObjectExtra("smd.runtimeSdk");
                if (lease instanceof RuntimeSdkScope.Lease) ((RuntimeSdkScope.Lease) lease).close();
            }
        });
    }

    private static void hookRuntimeSdkReaders() {
        for (Method method : ClassLoader.class.getDeclaredMethods()) {
            Class<?>[] types = method.getParameterTypes();
            if (!method.getName().equals("loadClass") || types.length == 0 || types[0] != String.class) continue;
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (runtimeSdkGuardNeeded && (runtimeInfoClass((String) param.args[0])
                        || RuntimeInitializerPolicy.isLibraryClass((String) param.args[0]))) {
                        param.setObjectExtra("smd.runtimeSdk", BuildHooks.useRuntimeSdk());
                    }
                }
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    Object lease = param.getObjectExtra("smd.runtimeSdk");
                    if (lease == null) return;
                    try {
                        if (param.getResult() instanceof Class) {
                            Class<?> type = (Class<?>) param.getResult();
                            if (RuntimeInitializerPolicy.isLibraryClass(type.getName())) installNativeRuntimeInitializers(type);
                            else installRuntimeSdkReaders(type);
                        }
                    } finally { ((RuntimeSdkScope.Lease) lease).close(); }
                }
            });
        }
    }

    private static void hookDefaultUserAgent(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> webSettingsClass = XposedHelpers.findClassIfExists(
            "android.webkit.WebSettings", lpparam.classLoader);
        if (webSettingsClass == null) {
            webSettingsClass = XposedHelpers.findClassIfExists("android.webkit.WebSettings", null);
        }
        if (webSettingsClass != null) {
            try {
                XposedHelpers.findAndHookMethod(webSettingsClass, "getDefaultUserAgent", Context.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            String spoofedUA = ConfigManager.getWebViewUserAgent();
                            if (spoofedUA != null && !spoofedUA.trim().isEmpty()) {
                                param.setResult(spoofedUA);
                            }
                        }
                    });
            } catch (Throwable ignored) {
            }
        }
    }

    private static void hookWebSettings(XC_LoadPackage.LoadPackageParam lpparam) {
        // Don't hook WebSettings directly - it's abstract
        // Instead, hook the implementation class used by WebView
        // We hook it indirectly through WebView.getSettings()

        Class<?> webViewClass = XposedHelpers.findClassIfExists(
            "android.webkit.WebView", lpparam.classLoader);

        if (webViewClass == null) {
            return;
        }

        try {
            // Hook WebView.getSettings() to intercept and modify the returned WebSettings object
            XposedHelpers.findAndHookMethod(webViewClass, "getSettings",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object settings = param.getResult();
                        if (settings != null) {
                            // Hook getUserAgentString() on the actual implementation class
                            Class<?> settingsClass = settings.getClass();

                            // Set spoofed UA immediately
                            String spoofedUA = ConfigManager.getWebViewUserAgent();
                            if (spoofedUA != null) {
                                try {
                                    XposedHelpers.callMethod(settings, "setUserAgentString", spoofedUA);
                                } catch (Exception e) {
                                    // Failed to set, that's okay
                                }
                            }

                            // getSettings() can run repeatedly; one callback per implementation
                            // prevents growing Xposed chains throughout the app's lifetime.
                            if (!USER_AGENT_CLASSES.add(settingsClass)) return;
                            try {
                                XposedHelpers.findAndHookMethod(settingsClass, "getUserAgentString",
                                    new XC_MethodHook() {
                                        @Override
                                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                            String targetUA = ConfigManager.getWebViewUserAgent();
                                            if (targetUA != null && !targetUA.trim().isEmpty()) {
                                                param.setResult(targetUA);
                                            }
                                        }
                                    });
                            } catch (Throwable ignored) {
                                USER_AGENT_CLASSES.remove(settingsClass);
                            }
                        }
                    }
                });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook WebView.getSettings(): " + e.getMessage());
        }
    }

    private static void hookWebViewConstructor(XC_LoadPackage.LoadPackageParam lpparam) {
        Class<?> webViewClass = XposedHelpers.findClassIfExists(
            "android.webkit.WebView", lpparam.classLoader);

        if (webViewClass == null) {
            return;
        }

        try {
            // Hook WebView(Context) constructor
            XposedHelpers.findAndHookConstructor(webViewClass,
                Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            Object webView = param.thisObject;
                            Object settings = XposedHelpers.callMethod(webView, "getSettings");

                            String spoofedUA = ConfigManager.getWebViewUserAgent();
                            if (spoofedUA != null) {
                                XposedHelpers.callMethod(settings, "setUserAgentString", spoofedUA);
                            }
                        } catch (Exception e) {
                            // Failed to set UA in constructor, that's okay
                        }
                    }
                });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook WebView constructor: " + e.getMessage());
        }

        try {
            // Hook WebView(Context, AttributeSet) constructor
            XposedHelpers.findAndHookConstructor(webViewClass,
                Context.class, android.util.AttributeSet.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            Object webView = param.thisObject;
                            Object settings = XposedHelpers.callMethod(webView, "getSettings");

                            String spoofedUA = ConfigManager.getWebViewUserAgent();
                            if (spoofedUA != null) {
                                XposedHelpers.callMethod(settings, "setUserAgentString", spoofedUA);
                            }
                        } catch (Exception e) {
                            // Failed to set UA in constructor, that's okay
                        }
                    }
                });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook WebView(Context, AttributeSet) constructor: " + e.getMessage());
        }

        try {
            // Hook WebView(Context, AttributeSet, int) constructor
            XposedHelpers.findAndHookConstructor(webViewClass,
                Context.class, android.util.AttributeSet.class, int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            Object webView = param.thisObject;
                            Object settings = XposedHelpers.callMethod(webView, "getSettings");

                            String spoofedUA = ConfigManager.getWebViewUserAgent();
                            if (spoofedUA != null) {
                                XposedHelpers.callMethod(settings, "setUserAgentString", spoofedUA);
                            }
                        } catch (Exception e) {
                            // Failed to set UA in constructor, that's okay
                        }
                    }
                });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook WebView(Context, AttributeSet, int) constructor: " + e.getMessage());
        }
    }
}
