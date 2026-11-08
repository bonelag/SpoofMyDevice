package com.devicespooflab.hooks.hooks;

import android.app.ActivityManager;

import com.devicespooflab.hooks.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HardwareHooks {

    private static final String TAG = "DeviceSpoofLab-Hardware";

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            hookRuntimeCores(lpparam);
            hookActivityManagerMemory(lpparam);
            hookGnssLocation(lpparam);
            XposedBridge.log(TAG + ": Successfully hooked hardware specs for " + lpparam.packageName);
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook hardware: " + e.getMessage());
        }
    }

    private static void hookRuntimeCores(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(Runtime.class, "availableProcessors",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        int cores = ConfigManager.getCpuCores();
                        if (cores > 0 && ConfigManager.isSpoofEnabled(ConfigManager.FIELD_CPU_CORES)) {
                            param.setResult(cores);
                        }
                    }
                });
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook Runtime.availableProcessors(): " + e.getMessage());
        }
    }

    private static void hookActivityManagerMemory(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> activityManagerClass = XposedHelpers.findClassIfExists(
                "android.app.ActivityManager", lpparam.classLoader);
            if (activityManagerClass == null) {
                return;
            }

            XposedHelpers.findAndHookMethod(activityManagerClass, "getMemoryInfo",
                ActivityManager.MemoryInfo.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ActivityManager.MemoryInfo memInfo = (ActivityManager.MemoryInfo) param.args[0];
                        long ramBytes = ConfigManager.hardwareIdentity().ramBytes;
                        if (memInfo != null && ConfigManager.hardwareIdentity().enabled("ram")) {
                            long originalTotal = memInfo.totalMem;
                            if (originalTotal > 0) {
                                double usedRatio = 1.0 - ((double) memInfo.availMem / originalTotal);
                                memInfo.availMem = (long) (ramBytes * (1.0 - usedRatio));
                            }
                            memInfo.totalMem = ramBytes;
                        }
                    }
                });

            XposedHelpers.findAndHookMethod(activityManagerClass, "getMemoryClass",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (ConfigManager.hardwareIdentity().enabled("ram")) param.setResult(512);
                    }
                });

            XposedHelpers.findAndHookMethod(activityManagerClass, "getLargeMemoryClass",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (ConfigManager.hardwareIdentity().enabled("ram")) param.setResult(1024);
                    }
                });

        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook ActivityManager memory: " + e.getMessage());
        }
    }

    private static void hookGnssLocation(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> locationManagerClass = XposedHelpers.findClassIfExists(
                "android.location.LocationManager", lpparam.classLoader);
            if (locationManagerClass == null) {
                locationManagerClass = XposedHelpers.findClassIfExists("android.location.LocationManager", null);
            }
            if (locationManagerClass == null) {
                return;
            }

            try {
                XposedHelpers.findAndHookMethod(locationManagerClass, "getGnssHardwareModelName",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            String gnssModel = ConfigManager.getGnssHardwareModelName();
                            if (gnssModel != null) {
                                param.setResult(gnssModel);
                            }
                        }
                    });
            } catch (Throwable ignored) {
            }

            try {
                XposedHelpers.findAndHookMethod(locationManagerClass, "getGnssYearOfHardware",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            int year = ConfigManager.getGnssYearOfHardware();
                            if (year > 0) {
                                param.setResult(year);
                            }
                        }
                    });
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }
    }
}
