package com.devicespooflab.hooks.hooks;

import android.hardware.Sensor;
import android.system.StructUtsname;
import com.devicespooflab.hooks.utils.ConfigManager;
import com.devicespooflab.hooks.utils.HardwareIdentity;
import com.devicespooflab.hooks.utils.ScopedHardwareRuntime;
import java.io.File;
import java.net.NetworkInterface;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** Common identity interfaces; no private target-package classes or UI substitutions. */
public final class IdentitySurfaceHooks {
    private IdentitySurfaceHooks() { }
    public static void hook(XC_LoadPackage.LoadPackageParam packageParam) {
        hookFiles(packageParam.classLoader);
        hookSensors();
        hookUname(packageParam.classLoader);
        hookNetwork(packageParam.classLoader);
        hookStorage(packageParam.classLoader);
    }

    private static void hookFiles(ClassLoader loader) {
        XC_MethodHook redirect = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                redirectReadArgument(param);
            }
        };
        for (String name : new String[]{"java.io.FileInputStream", "java.io.FileReader", "java.io.RandomAccessFile"}) {
            try {
                Class<?> type = XposedHelpers.findClassIfExists(name, loader);
                if (type == null) continue;
                for (java.lang.reflect.Constructor<?> constructor : type.getDeclaredConstructors()) {
                    Class<?>[] arguments = constructor.getParameterTypes();
                    if (arguments.length == 0 || (arguments[0] != String.class && arguments[0] != File.class)) continue;
                    if (name.endsWith("RandomAccessFile") && arguments.length >= 2) {
                        XposedBridge.hookMethod(constructor, new XC_MethodHook() {
                            @Override protected void beforeHookedMethod(MethodHookParam param) {
                                if ("r".equals(param.args[1])) redirectReadArgument(param);
                            }
                        });
                    } else XposedBridge.hookMethod(constructor, redirect);
                }
            } catch (Throwable ignored) { }
        }
        try {
            XposedHelpers.findAndHookMethod("libcore.io.IoBridge", loader, "open", String.class, int.class, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    int flags = (Integer) param.args[1];
                    if ((flags & 3) == 0 && (flags & (64 | 512)) == 0) {
                        String target = ScopedHardwareRuntime.current().redirect((String) param.args[0]);
                        if (ConfigManager.hasAssignedProfile() && target != null) param.args[0] = target;
                    }
                }
            });
        } catch (Throwable ignored) { }
        for (String name : new String[]{"open", "access", "stat", "lstat", "readlink"}) {
            try {
                Class<?> linux = XposedHelpers.findClassIfExists("libcore.io.Linux", loader);
                if (linux == null) continue;
                for (java.lang.reflect.Method method : linux.getDeclaredMethods()) {
                    Class<?>[] arguments = method.getParameterTypes();
                    if (!method.getName().equals(name) || arguments.length == 0 || arguments[0] != String.class) continue;
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override protected void beforeHookedMethod(MethodHookParam param) {
                            if (name.equals("open") && param.args.length > 1) {
                                int flags = (Integer) param.args[1];
                                if ((flags & 3) != 0 || (flags & (64 | 512)) != 0) return;
                            }
                            if (name.equals("access") && param.args.length > 1 && (((Integer) param.args[1]) & 2) != 0) return;
                            redirectReadArgument(param);
                        }
                    });
                }
            } catch (Throwable ignored) { }
        }
        for (String method : new String[]{"exists", "isDirectory", "isFile", "canRead", "list", "listFiles"}) {
            try {
                XposedHelpers.findAndHookMethod(File.class, method, new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        if (!ConfigManager.hasAssignedProfile()) return;
                        File original = (File) param.thisObject;
                        String path = ScopedHardwareRuntime.current().redirect(original.getPath());
                        if (path == null) return;
                        File target = new File(path);
                        if (method.equals("exists")) param.setResult(target.exists());
                        else if (method.equals("isDirectory")) param.setResult(target.isDirectory());
                        else if (method.equals("isFile")) param.setResult(target.isFile());
                        else if (method.equals("canRead")) param.setResult(target.canRead());
                        else if (method.equals("list")) param.setResult(target.list());
                        else {
                            String[] names = target.list();
                            if (names == null) { param.setResult(null); return; }
                            File[] files = new File[names.length];
                            for (int index = 0; index < names.length; index++) files[index] = new File(original, names[index]);
                            param.setResult(files);
                        }
                    }
                });
            } catch (Throwable ignored) { }
        }
    }

    private static void redirectReadArgument(XC_MethodHook.MethodHookParam param) {
        if (!ConfigManager.hasAssignedProfile() || param.args.length == 0) return;
        Object argument = param.args[0];
        String path = argument instanceof File ? ((File) argument).getPath() : argument instanceof String ? (String) argument : null;
        String target = ScopedHardwareRuntime.current().redirect(path);
        if (target != null) param.args[0] = argument instanceof File ? new File(target) : target;
    }

    private static void hookSensors() {
        for (String name : new String[]{"getName", "getVendor", "getVersion"}) {
            try {
                XposedHelpers.findAndHookMethod(Sensor.class, name, new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam param) {
                        HardwareIdentity identity = ConfigManager.hardwareIdentity();
                        if (!identity.enabled("sensor_metadata")) return;
                        int type = ((Sensor) param.thisObject).getType();
                        param.setResult(name.equals("getName") ? identity.sensorName(type) : name.equals("getVendor")
                            ? identity.sensorVendor(type) : 1);
                    }
                });
            } catch (Throwable ignored) { }
        }
    }

    private static void hookUname(ClassLoader loader) {
        try {
            XposedHelpers.findAndHookMethod("libcore.io.Linux", loader, "uname", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    HardwareIdentity identity = ConfigManager.hardwareIdentity();
                    if (identity.enabled("kernel")) param.setResult(new StructUtsname("Linux", identity.value("kernel.hostname", "localhost"),
                        identity.kernelRelease(), identity.kernelVersion(), identity.machine()));
                }
            });
        } catch (Throwable ignored) { }
    }

    private static void hookNetwork(ClassLoader loader) {
        for (String type : new String[]{"android.net.wifi.WifiInfo", "android.bluetooth.BluetoothAdapter"}) {
            String method = type.endsWith("WifiInfo") ? "getMacAddress" : "getAddress";
            try {
                XposedHelpers.findAndHookMethod(type, loader, method, new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam param) {
                        if (!ConfigManager.hardwareIdentity().enabled("network_metadata")) return;
                        param.setResult(mac(type.endsWith("WifiInfo") ? "wifi" : "bluetooth"));
                    }
                });
            } catch (Throwable ignored) { }
        }
        try {
            XposedHelpers.findAndHookMethod("android.bluetooth.BluetoothAdapter", loader, "getName", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    HardwareIdentity identity = ConfigManager.hardwareIdentity();
                    if (identity.enabled("model")) param.setResult(identity.marketingName());
                }
            });
        } catch (Throwable ignored) { }
        try {
            XposedHelpers.findAndHookMethod(NetworkInterface.class, "getHardwareAddress", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    if (!ConfigManager.hardwareIdentity().enabled("network_metadata")) return;
                    String name = ((NetworkInterface) param.thisObject).getName();
                    if (!name.startsWith("wlan") && !name.startsWith("wifi")) return;
                    String[] values = mac("wifi").split(":");
                    byte[] address = new byte[6];
                    if (values.length != address.length) return;
                    try { for (int index = 0; index < values.length; index++) address[index] = (byte) Integer.parseInt(values[index], 16); }
                    catch (NumberFormatException ignored) { return; }
                    param.setResult(address);
                }
            });
        } catch (Throwable ignored) { }
    }

    private static String mac(String interfaceName) {
        return ConfigManager.hardwareIdentity().macAddress(interfaceName);
    }

    private static void hookStorage(ClassLoader loader) {
        try {
            XposedHelpers.findAndHookMethod("android.os.StatFs", loader, "getTotalBytes", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    HardwareIdentity identity = ConfigManager.hardwareIdentity();
                    if (!identity.enabled("storage_metadata")) return;
                    try { param.setResult(Long.parseLong(identity.value("hardware.storage_bytes", "274877906944"))); }
                    catch (NumberFormatException ignored) { }
                }
            });
        } catch (Throwable ignored) { }
    }
}
