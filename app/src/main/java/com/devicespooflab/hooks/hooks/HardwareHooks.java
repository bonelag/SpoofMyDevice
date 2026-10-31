package com.devicespooflab.hooks.hooks;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;

import com.devicespooflab.hooks.utils.ConfigManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HardwareHooks {

    private static final String TAG = "DeviceSpoofLab-Hardware";
    private static final long DEFAULT_RAM_BYTES = 12L * 1024 * 1024 * 1024; // 12GB
    private static final long DEFAULT_RAM_KB = 12L * 1024 * 1024; // 12GB in KB

    private static volatile File appSmdDir = null;

    public static void init(Context context) {
        if (context == null) return;
        try {
            File cacheDir = context.getCacheDir();
            if (cacheDir == null) return;
            File smdDir = new File(cacheDir, ".smd");
            if (!smdDir.exists()) {
                smdDir.mkdirs();
            }
            File cpuInfoFile = new File(smdDir, "cpuinfo");
            writeTextFile(cpuInfoFile, ConfigManager.getCpuInfoContent());
            ConfigManager.setAppCpuInfoFile(cpuInfoFile);

            int cores = ConfigManager.getCpuCores();
            String coreRange = "0-" + (cores > 0 ? cores - 1 : 7) + "\n";
            writeTextFile(new File(smdDir, "present"), coreRange);
            writeTextFile(new File(smdDir, "online"), coreRange);
            writeTextFile(new File(smdDir, "possible"), coreRange);
            writeTextFile(new File(smdDir, "cache_l3"), ConfigManager.getCacheL3Size() + "\n");
            writeTextFile(new File(smdDir, "max_freq"), ConfigManager.getCpuMaxFreq() + "\n");
            writeTextFile(new File(smdDir, "min_freq"), ConfigManager.getCpuMinFreq() + "\n");
            appSmdDir = smdDir;
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Failed to init cache files: " + t.getMessage());
        }
    }

    private static void writeTextFile(File file, String content) {
        try (FileOutputStream fos = new FileOutputStream(file, false)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (Throwable ignored) {
        }
    }

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            hookRuntimeCores();
            hookOsUname();
            hookActivityManagerMemory(lpparam);
            hookDebugMemory();
            hookFileReads();
            hookGnssLocation(lpparam);
            hookDeviceInfoHw(lpparam);
            XposedBridge.log(TAG + ": Successfully hooked hardware specs");
        } catch (Exception e) {
            XposedBridge.log(TAG + ": Failed to hook hardware: " + e.getMessage());
        }
    }

    private static void hookRuntimeCores() {
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

    private static void hookOsUname() {
        XC_MethodHook unameHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object original = param.getResult();
                if (original != null) {
                    String abi = ConfigManager.getCpuAbi();
                    String machine = (abi != null && abi.contains("arm64")) ? "aarch64" : "armv7l";
                    try {
                        XposedHelpers.setObjectField(original, "machine", machine);
                    } catch (Throwable ignored) {
                    }
                }
            }
        };

        try {
            Class<?> osClass = XposedHelpers.findClassIfExists("android.system.Os", null);
            if (osClass != null) {
                XposedHelpers.findAndHookMethod(osClass, "uname", unameHook);
            }
        } catch (Throwable ignored) {
        }

        try {
            Class<?> forwardingOsClass = XposedHelpers.findClassIfExists("libcore.io.ForwardingOs", null);
            if (forwardingOsClass != null) {
                XposedHelpers.findAndHookMethod(forwardingOsClass, "uname", unameHook);
            }
        } catch (Throwable ignored) {
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
                            memInfo.totalMem = DEFAULT_RAM_BYTES;
                            long originalTotal = memInfo.totalMem;
                            if (originalTotal > 0) {
                                double usedRatio = 1.0 - ((double) memInfo.availMem / originalTotal);
                                memInfo.availMem = (long) (DEFAULT_RAM_BYTES * (1.0 - usedRatio));
                            }
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

    private static File getRedirectedFile(String path) {
        if (path == null) return null;
        File dir = appSmdDir;
        if (dir == null || !dir.exists()) {
            File fallback = new File("/data/local/tmp/spoofmydevice");
            if (fallback.exists()) {
                dir = fallback;
            }
        }
        if (dir == null || !dir.exists()) return null;

        if (path.equals("/proc/cpuinfo")) {
            File f = new File(dir, "cpuinfo");
            if (f.exists()) return f;
        }
        if (path.equals("/sys/devices/system/cpu/present")) {
            File f = new File(dir, "present");
            if (f.exists()) return f;
        }
        if (path.equals("/sys/devices/system/cpu/online")) {
            File f = new File(dir, "online");
            if (f.exists()) return f;
        }
        if (path.equals("/sys/devices/system/cpu/possible")) {
            File f = new File(dir, "possible");
            if (f.exists()) return f;
        }
        if (path.contains("/cache/index3/size")) {
            File f = new File(dir, "cache_l3");
            if (f.exists()) return f;
        }
        if (path.contains("cpuinfo_max_freq") || path.contains("scaling_max_freq") || path.contains("scaling_cur_freq")) {
            File f = new File(dir, "max_freq");
            if (f.exists()) return f;
        }
        if (path.contains("cpuinfo_min_freq") || path.contains("scaling_min_freq")) {
            File f = new File(dir, "min_freq");
            if (f.exists()) return f;
        }
        return null;
    }

    private static void hookFileReads() {
        try {
            XposedHelpers.findAndHookConstructor(FileInputStream.class, File.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        File file = (File) param.args[0];
                        if (file != null) {
                            File redir = getRedirectedFile(file.getPath());
                            if (redir != null) {
                                param.args[0] = redir;
                            }
                        }
                    }
                });
        } catch (Throwable ignored) {
        }

        try {
            XposedHelpers.findAndHookConstructor(FileInputStream.class, String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String path = (String) param.args[0];
                        if (path != null) {
                            File redir = getRedirectedFile(path);
                            if (redir != null) {
                                param.args[0] = redir.getAbsolutePath();
                            }
                        }
                    }
                });
        } catch (Throwable ignored) {
        }

        try {
            XposedHelpers.findAndHookConstructor(RandomAccessFile.class, File.class, String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        File file = (File) param.args[0];
                        if (file != null) {
                            File redir = getRedirectedFile(file.getPath());
                            if (redir != null) {
                                param.args[0] = redir;
                            }
                        }
                    }
                });
        } catch (Throwable ignored) {
        }

        try {
            XposedHelpers.findAndHookConstructor(RandomAccessFile.class, String.class, String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String path = (String) param.args[0];
                        if (path != null) {
                            File redir = getRedirectedFile(path);
                            if (redir != null) {
                                param.args[0] = redir.getAbsolutePath();
                            }
                        }
                    }
                });
        } catch (Throwable ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(RandomAccessFile.class, "readLine",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String line = (String) param.getResult();
                        if (line != null && line.startsWith("MemTotal:")) {
                            param.setResult("MemTotal:       " + DEFAULT_RAM_KB + " kB");
                        }
                    }
                });
        } catch (Throwable ignored) {
        }

        try {
            XposedHelpers.findAndHookMethod(File.class, "exists",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        File file = (File) param.thisObject;
                        String path = file.getPath();
                        if ("/proc/cpuinfo".equals(path) || (path != null && path.startsWith("/sys/devices/system/cpu/"))) {
                            param.setResult(true);
                        }
                    }
                });
        } catch (Throwable ignored) {
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
