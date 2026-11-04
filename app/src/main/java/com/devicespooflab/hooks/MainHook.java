package com.devicespooflab.hooks;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.devicespooflab.hooks.hooks.AdvertisingIdHooks;
import com.devicespooflab.hooks.hooks.AppSetIdHooks;
import com.devicespooflab.hooks.hooks.BuildHooks;
import com.devicespooflab.hooks.hooks.DisplayHooks;
import com.devicespooflab.hooks.hooks.EmulatorDetectionHooks;
import com.devicespooflab.hooks.hooks.GetPropHooks;
import com.devicespooflab.hooks.hooks.HardwareHooks;
import com.devicespooflab.hooks.hooks.JavaSystemPropertyHooks;
import com.devicespooflab.hooks.hooks.LocaleHooks;
import com.devicespooflab.hooks.hooks.MediaDrmHooks;
import com.devicespooflab.hooks.hooks.OpenGLHooks;
import com.devicespooflab.hooks.hooks.PackageManagerHooks;
import com.devicespooflab.hooks.hooks.SettingsHooks;
import com.devicespooflab.hooks.hooks.SystemPropertiesHooks;
import com.devicespooflab.hooks.hooks.TelephonyHooks;
import com.devicespooflab.hooks.hooks.VendorSystemPropertiesHooks;
import com.devicespooflab.hooks.hooks.WebViewHooks;
import com.devicespooflab.hooks.utils.ConfigManager;
import com.devicespooflab.hooks.utils.HookSafetyPolicy;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Main entry point for DeviceSpoofLabs-Hooks LSPosed module.
 *
 * STANDALONE MODULE - No Magisk dependency.
 *
 * This module hooks Android APIs to spoof device identifiers and properties
 * that some apps read directly from Java APIs or SystemProperties.
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "SpoofMyDevice";
    private static final String MODULE_PACKAGE = "com.spoofmydevice";
    private static final int REAL_SDK_INT = Build.VERSION.SDK_INT;
    private static final Set<String> INITIALIZED_HOOK_PROCESSES = ConcurrentHashMap.newKeySet();
    private static final Set<String> CONFIG_RECEIVER_PROCESSES = ConcurrentHashMap.newKeySet();
    private static final Map<String, HookSession> HOOK_SESSIONS = new ConcurrentHashMap<>();

    private static boolean isBootReady() {
        try {
            // Read fresh, original properties. Never trust a cached true value or our own hooks.
            Method getter = BootPropertyReader.GETTER;
            String completed = (String) XposedBridge.invokeOriginalMethod(
                getter, null, new Object[]{"sys.boot_completed"});
            String animation = (String) XposedBridge.invokeOriginalMethod(
                getter, null, new Object[]{"init.svc.bootanim"});
            return HookSafetyPolicy.isBootReady(completed, animation);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isSystemCriticalPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam.appInfo == null) {
            return true;
        }
        try {
            return HookSafetyPolicy.isProtectedProcess(
                lpparam.packageName,
                lpparam.processName,
                android.os.Process.myUid(),
                lpparam.appInfo.flags
            );
        } catch (Throwable ignored) {
            return true;
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (MODULE_PACKAGE.equals(lpparam.packageName)) {
            hookActivationCheck(lpparam);
            return;
        }

        // A package loaded by another app or a system host must not configure that process.
        if (!lpparam.isFirstApplication || isSystemCriticalPackage(lpparam)) {
            return;
        }

        // No preferences, providers, workers, or Application hooks during boot.
        // Apps launched in this window keep real values until their next process start.
        if (!isBootReady()) {
            return;
        }

        String processName = lpparam.processName == null ? lpparam.packageName : lpparam.processName;
        HookSession session = new HookSession(lpparam);
        if (HOOK_SESSIONS.putIfAbsent(processName, session) != null) {
            return;
        }
        ConfigManager.init(lpparam.packageName);
        hookApplicationAttachForInitialization(lpparam, session);
        session.requestReload();
    }

    private void initializeHooks(XC_LoadPackage.LoadPackageParam lpparam) {
        String processName = lpparam.processName == null ? lpparam.packageName : lpparam.processName;
        if (!INITIALIZED_HOOK_PROCESSES.add(processName)) {
            return;
        }

        // Capture real fields before SystemProperties hooks can affect Build initialization.
        BuildHooks.captureOriginals(lpparam.classLoader);

        try {
            SystemPropertiesHooks.hook(lpparam);
            XposedBridge.log(TAG + ": SystemPropertiesHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": SystemPropertiesHooks failed: " + exception.getMessage());
        }

        try {
            VendorSystemPropertiesHooks.hook(lpparam);
            XposedBridge.log(TAG + ": VendorSystemPropertiesHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": VendorSystemPropertiesHooks failed: " + exception.getMessage());
        }

        try {
            JavaSystemPropertyHooks.hook(lpparam);
            XposedBridge.log(TAG + ": JavaSystemPropertyHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": JavaSystemPropertyHooks failed: " + exception.getMessage());
        }

        try {
            GetPropHooks.hook(lpparam);
            XposedBridge.log(TAG + ": GetPropHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": GetPropHooks failed: " + exception.getMessage());
        }

        try {
            LocaleHooks.hook(lpparam);
            XposedBridge.log(TAG + ": LocaleHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": LocaleHooks failed: " + exception.getMessage());
        }

        try {
            HardwareHooks.hook(lpparam);
            XposedBridge.log(TAG + ": HardwareHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": HardwareHooks failed: " + exception.getMessage());
        }

        try {
            OpenGLHooks.hook(lpparam);
            XposedBridge.log(TAG + ": OpenGLHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": OpenGLHooks failed: " + exception.getMessage());
        }

        try {
            DisplayHooks.hook(lpparam);
            XposedBridge.log(TAG + ": DisplayHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": DisplayHooks failed: " + exception.getMessage());
        }

        try {
            EmulatorDetectionHooks.hook(lpparam);
            XposedBridge.log(TAG + ": EmulatorDetectionHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": EmulatorDetectionHooks failed: " + exception.getMessage());
        }

        try {
            TelephonyHooks.hook(lpparam);
            XposedBridge.log(TAG + ": TelephonyHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": TelephonyHooks failed: " + exception.getMessage());
        }

        try {
            SettingsHooks.hook(lpparam);
            XposedBridge.log(TAG + ": SettingsHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": SettingsHooks failed: " + exception.getMessage());
        }

        try {
            AdvertisingIdHooks.hook(lpparam);
            XposedBridge.log(TAG + ": AdvertisingIdHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": AdvertisingIdHooks failed: " + exception.getMessage());
        }

        if (REAL_SDK_INT >= 30) {
            try {
                AppSetIdHooks.hook(lpparam);
                XposedBridge.log(TAG + ": AppSetIdHooks loaded");
            } catch (Exception exception) {
                XposedBridge.log(TAG + ": AppSetIdHooks failed: " + exception.getMessage());
            }
        }

        try {
            MediaDrmHooks.hook(lpparam);
            XposedBridge.log(TAG + ": MediaDrmHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": MediaDrmHooks failed: " + exception.getMessage());
        }

        try {
            WebViewHooks.hook(lpparam);
            XposedBridge.log(TAG + ": WebViewHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": WebViewHooks failed: " + exception.getMessage());
        }

        try {
            PackageManagerHooks.hook(lpparam);
            XposedBridge.log(TAG + ": PackageManagerHooks loaded");
        } catch (Exception exception) {
            XposedBridge.log(TAG + ": PackageManagerHooks failed: " + exception.getMessage());
        }

        // Resolve/install other hooks using the real Android version first.
        BuildHooks.hook(lpparam);

        XposedBridge.log(TAG + ": All hooks initialized for " + lpparam.packageName);
    }

    private void hookApplicationAttachForInitialization(
        XC_LoadPackage.LoadPackageParam lpparam,
        HookSession session
    ) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.Application",
                lpparam.classLoader,
                "attach",
                android.content.Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!isBootReady()) {
                            return;
                        }
                        try {
                            Context context = (Context) param.args[0];
                            if (!lpparam.packageName.equals(context.getPackageName())) {
                                return;
                            }
                            session.context = context;
                            // Prefetch may already have finished. This reads memory only.
                            session.applyCurrentConfig();
                            session.requestReload();
                        } catch (Throwable throwable) {
                            XposedBridge.log(TAG + ": Failed to initialize hooks after attach for " + lpparam.packageName + ": " + throwable.getMessage());
                        }
                    }
                }
            );
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + ": Failed to hook Application.attach initialization path: " + throwable.getMessage());
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerConfigChangeReceiver(Context context, HookSession session) {
        XC_LoadPackage.LoadPackageParam lpparam = session.lpparam;
        String processName = lpparam.processName == null ? lpparam.packageName : lpparam.processName;
        if (!CONFIG_RECEIVER_PROCESSES.add(processName)) {
            return;
        }

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context receiverContext, Intent intent) {
                if (intent == null || !ConfigManager.ACTION_CONFIG_CHANGED.equals(intent.getAction())) {
                    return;
                }
                // Return to Android immediately; do not start the provider while handling a broadcast.
                session.requestReload();
            }
        };

        try {
            IntentFilter filter = new IntentFilter(ConfigManager.ACTION_CONFIG_CHANGED);
            if (REAL_SDK_INT >= 33) {
                context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                context.registerReceiver(receiver, filter);
            }
        } catch (Throwable throwable) {
            CONFIG_RECEIVER_PROCESSES.remove(processName);
            XposedBridge.log(TAG + ": Config receiver registration failed for "
                + lpparam.packageName + ": " + throwable);
        }
    }

    private final class HookSession {
        private final XC_LoadPackage.LoadPackageParam lpparam;
        private final Handler mainHandler = new Handler(Looper.getMainLooper());
        private final AtomicBoolean reloadRequested = new AtomicBoolean();
        private final AtomicBoolean reloadRunning = new AtomicBoolean();
        private volatile Context context;

        private HookSession(XC_LoadPackage.LoadPackageParam lpparam) {
            this.lpparam = lpparam;
        }

        private void requestReload() {
            if (!isBootReady()) {
                return;
            }
            reloadRequested.set(true);
            if (reloadRunning.compareAndSet(false, true)) {
                ConfigWorker.EXECUTOR.execute(this::reloadOnWorker);
            }
        }

        private void reloadOnWorker() {
            try {
                do {
                    reloadRequested.set(false);
                    if (!isBootReady()) {
                        return;
                    }
                    try {
                        Context attachedContext = context;
                        if (attachedContext != null) {
                            registerConfigChangeReceiver(attachedContext, this);
                        }
                        if (ConfigManager.forceReload(attachedContext)) {
                            mainHandler.post(this::applyCurrentConfig);
                        }
                    } catch (Throwable throwable) {
                        XposedBridge.log(TAG + ": Background config load failed for "
                            + lpparam.packageName + ": " + throwable);
                    }
                } while (reloadRequested.get());
            } finally {
                reloadRunning.set(false);
                // Cover a change arriving between the final check and releasing the worker.
                if (reloadRequested.get()) {
                    requestReload();
                }
            }
        }

        private void applyCurrentConfig() {
            Context attachedContext = context;
            if (attachedContext == null || !isBootReady()) {
                return;
            }
            try {
                if (ConfigManager.hasAssignedProfile()) {
                    initializeHooks(lpparam);
                }
                String processName = lpparam.processName == null ? lpparam.packageName : lpparam.processName;
                if (INITIALIZED_HOOK_PROCESSES.contains(processName)) {
                    // Also restore original Build fields when a profile is removed.
                    BuildHooks.reapply(lpparam.classLoader, lpparam.packageName);
                }
            } catch (Throwable throwable) {
                XposedBridge.log(TAG + ": Applying cached config failed for "
                    + lpparam.packageName + ": " + throwable);
            }
        }
    }

    private static final class ConfigWorker {
        // Lazy holder: no worker is created when LSPosed loads MainHook during boot.
        private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "spoofmydevice-config");
            thread.setDaemon(true);
            return thread;
        });
    }

    private static final class BootPropertyReader {
        private static final Method GETTER = findGetter();

        private static Method findGetter() {
            try {
                Class<?> properties = Class.forName("android.os.SystemProperties");
                Method getter = properties.getDeclaredMethod("get", String.class);
                getter.setAccessible(true);
                return getter;
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Original boot properties unavailable", exception);
            }
        }
    }

    private void hookActivationCheck(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.devicespooflab.hooks.MainActivity",
                lpparam.classLoader,
                "isModuleActivated",
                XC_MethodReplacement.returnConstant(true)
            );
            XposedBridge.log(TAG + ": Activation check hooked for companion app");
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + ": Failed to hook activation check: " + throwable.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(
                "com.devicespooflab.hooks.ui.SpoofAppState",
                lpparam.classLoader,
                "isModuleActivated",
                XC_MethodReplacement.returnConstant(true)
            );
        } catch (Throwable ignored) {
        }
    }
}
