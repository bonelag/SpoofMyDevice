package com.devicespooflab.hooks.hooks;

import android.app.ActivityManager;
import android.os.Debug;

import com.devicespooflab.hooks.utils.ConfigManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HardwareHooks {

    private static final String TAG = "DeviceSpoofLab-Hardware";
    private static final long DEFAULT_RAM_BYTES = 12L * 1024 * 1024 * 1024; // 12GB

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            hookRuntimeCores(lpparam);
            hookActivityManagerMemory(lpparam);
            hookDebugMemory();
            hookGnssLocation(lpparam);
            hookDeviceInfoHw(lpparam);
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
                        if (cores > 0) {
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
                        if (memInfo != null) {
                            long originalTotal = memInfo.totalMem;
                            if (originalTotal > 0) {
                                double usedRatio = 1.0 - ((double) memInfo.availMem / originalTotal);
                                memInfo.availMem = (long) (DEFAULT_RAM_BYTES * (1.0 - usedRatio));
                            }
                            memInfo.totalMem = DEFAULT_RAM_BYTES;
                        }
                    }
                });

            XposedHelpers.findAndHookMethod(activityManagerClass, "getMemoryClass",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(512);
                    }
                });

            XposedHelpers.findAndHookMethod(activityManagerClass, "getLargeMemoryClass",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(1024);
                    }
                });

        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook ActivityManager memory: " + e.getMessage());
        }
    }

    private static void hookDebugMemory() {
        try {
            XposedHelpers.findAndHookMethod(Debug.class, "getNativeHeapSize",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        long originalSize = (Long) param.getResult();
                        param.setResult(originalSize * 4);
                    }
                });
        } catch (Exception ignored) {
        }
    }

    private static void hookDeviceInfoHw(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!"ru.andr7e.deviceinfohw".equals(lpparam.packageName)) {
            return;
        }
        try {
            Class<?> fileReaderClass = XposedHelpers.findClassIfExists("O0.e", lpparam.classLoader);
            if (fileReaderClass != null) {
                XC_MethodHook interceptReaderHook = new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String path = (String) param.args[0];
                        if (path == null) return;
                        if (path.equals("/proc/cpuinfo")) {
                            param.setResult(ConfigManager.getCpuInfoContent());
                        } else if (path.equals("/sys/devices/system/cpu/present")
                            || path.equals("/sys/devices/system/cpu/online")
                            || path.equals("/sys/devices/system/cpu/possible")) {
                            param.setResult("0-" + (ConfigManager.getCpuCores() - 1));
                        } else if (path.contains("/cache/index3/size")) {
                            param.setResult(ConfigManager.getCacheL3Size());
                        } else if (path.contains("cpuinfo_max_freq") || path.contains("scaling_max_freq") || path.contains("scaling_cur_freq")) {
                            param.setResult(ConfigManager.getCpuMaxFreq());
                        } else if (path.contains("cpuinfo_min_freq") || path.contains("scaling_min_freq")) {
                            param.setResult(ConfigManager.getCpuMinFreq());
                        }
                    }
                };

                XposedHelpers.findAndHookMethod(fileReaderClass, "b", String.class, interceptReaderHook);
                XposedHelpers.findAndHookMethod(fileReaderClass, "c", String.class, interceptReaderHook);
                try {
                    XposedHelpers.findAndHookMethod(fileReaderClass, "d", String.class, interceptReaderHook);
                } catch (Throwable ignored) {
                }
                try {
                    XposedHelpers.findAndHookMethod(fileReaderClass, "i", String.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            String path = (String) param.args[0];
                            if (path != null && (path.equals("/proc/cpuinfo") || path.startsWith("/sys/devices/system/cpu/"))) {
                                param.setResult(true);
                            }
                        }
                    });
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
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
