package com.devicespooflab.hooks.utils;

import android.content.Context;
import android.os.Bundle;

import com.devicespooflab.hooks.ConfigProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

/**
 * Manages configuration for spoofed values.
 *
 * Target processes load profiles on MainHook's background worker. Hook callbacks read
 * only immutable, already published snapshots; they never reload or perform IPC.
 */
public class ConfigManager {

    public static final String ACTION_CONFIG_CHANGED = "com.spoofmydevice.action.CONFIG_CHANGED";
    public static final String KEY_APPLY_SCREEN_METRICS = "device.apply_screen_metrics";
    public static final String KEY_SPOOF_IMEI = "device.imei";
    public static final String KEY_SPOOF_MEID = "device.meid";
    public static final String KEY_SPOOF_IMSI = "device.imsi";
    public static final String KEY_SPOOF_ICCID = "device.iccid";
    public static final String KEY_SPOOF_PHONE_NUMBER = "device.phone_number";
    public static final String KEY_SPOOF_GAID = "device.gaid";
    public static final String KEY_SPOOF_GSF_ID = "device.gsf_id";
    public static final String KEY_SPOOF_MEDIA_DRM_ID = "device.media_drm_id";
    public static final String KEY_SPOOF_APP_SET_ID = "device.app_set_id";
    public static final String KEY_SAFE_MODE_PACKAGES = "safe_mode.packages";
    public static final String KEY_DEFAULT_PROFILE_ID = "default_profile_id";
    public static final String KEY_SPOOF_TOGGLE_PREFIX = "spoof.enabled.";

    public static final String FIELD_BRAND = "brand";
    public static final String FIELD_MANUFACTURER = "manufacturer";
    public static final String FIELD_MODEL = "model";
    public static final String FIELD_DEVICE = "device";
    public static final String FIELD_PRODUCT = "product";
    public static final String FIELD_BOARD = "board";
    public static final String FIELD_HARDWARE = "hardware";
    public static final String FIELD_BOARD_PLATFORM = "board_platform";
    public static final String FIELD_ANDROID_RELEASE = "android_release";
    public static final String FIELD_SDK = "sdk";
    public static final String FIELD_SECURITY_PATCH = "security_patch";
    public static final String FIELD_BUILD_ID = "build_id";
    public static final String FIELD_BUILD_DISPLAY_ID = "build_display_id";
    public static final String FIELD_BUILD_INCREMENTAL = "build_incremental";
    public static final String FIELD_FINGERPRINT = "fingerprint";
    public static final String FIELD_SCREEN_WIDTH = "screen_width";
    public static final String FIELD_SCREEN_HEIGHT = "screen_height";
    public static final String FIELD_SCREEN_DENSITY = "screen_density";
    public static final String FIELD_OPERATOR_ALPHA = "operator_alpha";
    public static final String FIELD_OPERATOR_NUMERIC = "operator_numeric";
    public static final String FIELD_SIM_COUNTRY = "sim_country";
    public static final String FIELD_TIMEZONE = "timezone";
    public static final String FIELD_LOCALE = "locale";
    public static final String FIELD_ANDROID_ID = "android_id";
    public static final String FIELD_IMEI = "imei";
    public static final String FIELD_MEID = "meid";
    public static final String FIELD_IMSI = "imsi";
    public static final String FIELD_ICCID = "iccid";
    public static final String FIELD_PHONE_NUMBER = "phone_number";
    public static final String FIELD_GAID = "gaid";
    public static final String FIELD_GSF_ID = "gsf_id";
    public static final String FIELD_MEDIA_DRM_ID = "media_drm_id";
    public static final String FIELD_APP_SET_ID = "app_set_id";
    public static final String FIELD_BASEBAND = "baseband";
    public static final String FIELD_BUILD_USER = "build_user";
    public static final String FIELD_BUILD_HOST = "build_host";
    public static final String FIELD_BOOTLOADER = "bootloader";
    public static final String FIELD_SOC_MODEL = "soc_model";
    public static final String FIELD_SOC_MANUFACTURER = "soc_manufacturer";
    public static final String FIELD_BUILD_DATE_UTC = "build_date_utc";
    public static final String FIELD_BUILD_DATE = "build_date";
    public static final String FIELD_GNSS_MODEL = "gnss_model";
    public static final String FIELD_GNSS_YEAR = "gnss_year";
    public static final String FIELD_BUILD_DESCRIPTION = "build_description";
    public static final String FIELD_BUILD_FLAVOR = "build_flavor";
    public static final String FIELD_BUILD_PRODUCT = "build_product";
    public static final String FIELD_BUILD_CHARACTERISTICS = "build_characteristics";
    public static final String FIELD_SERIAL_NUMBER = "serial_number";
    public static final String FIELD_CPU_ABI = "cpu_abi";
    public static final String FIELD_CPU_ABI_LIST = "cpu_abi_list";
    public static final String FIELD_CPU_ABI_LIST_64 = "cpu_abi_list_64";
    public static final String FIELD_CPU_ABI_LIST_32 = "cpu_abi_list_32";
    public static final String FIELD_USER_AGENT = "user_agent";
    public static final String FIELD_SIM_OPERATOR_ALPHA = "sim_operator_alpha";
    public static final String FIELD_SIM_OPERATOR_NUMERIC = "sim_operator_numeric";
    public static final String FIELD_GPU_VENDOR = "gpu_vendor";
    public static final String FIELD_GPU_RENDERER = "gpu_renderer";
    public static final String FIELD_GL_VERSION = "gl_version";
    public static final String FIELD_GLSL_VERSION = "glsl_version";
    public static final String FIELD_VULKAN_VERSION = "vulkan_version";
    public static final String FIELD_VULKAN_DRIVER_VERSION = "vulkan_driver_version";
    public static final String FIELD_VULKAN_LEVEL = "vulkan_level";
    public static final String FIELD_CPU_HARDWARE = "cpu_hardware";
    public static final String FIELD_CPU_CORES = "cpu_cores";

    private static final RuntimeConfigStore STORE = new RuntimeConfigStore(
        new RuntimeConfigStore.Snapshot(getEmbeddedDefaults(), true)
    );
    private static volatile String targetPackageName;
    private static final int MAX_PROFILE_BYTES = 4 * 1024 * 1024;

    /** Sets the target only. Preference and provider reads must run on the config worker. */
    public static void init(String packageName) {
        targetPackageName = packageName;
    }

    public static boolean forceReload() {
        return forceReload(null);
    }

    /** Caller must be a background worker, never Application.attach or a hook callback. */
    public static boolean forceReload(Context context) {
        return STORE.reload(() -> readConfig(context));
    }

    public static boolean isUsingEmbeddedDefaults() {
        return STORE.current().fromEmbeddedDefaults;
    }

    public static boolean hasAssignedProfile() {
        return STORE.current().hasAssignedProfile();
    }

    private static RuntimeConfigStore.Snapshot readConfig(Context preferredContext) {
        String packageName = targetPackageName;
        boolean allowDefault = !isSystemPackage(packageName);
        // All transports run on the worker, even XSharedPreferences constructors/getters.
        try {
            RuntimeConfigStore.Snapshot configFromXsp = readFromXSharedPrefs(packageName, allowDefault);
            if (configFromXsp != null) {
                return configFromXsp;
            }
        } catch (Throwable t) {
            XposedBridge.log("SpoofMyDevice: readFromXSharedPrefs failed: " + t.getMessage());
        }

        // 2. Try direct file read
        try {
            RuntimeConfigStore.Snapshot configFromDisk = readFromDirectFiles(packageName, allowDefault);
            if (configFromDisk != null) {
                return configFromDisk;
            }
        } catch (Throwable t) {
            XposedBridge.log("SpoofMyDevice: readFromDirectFiles failed: " + t.getMessage());
        }

        // 3. Try ContentProvider
        try {
            RuntimeConfigStore.Snapshot configFromProvider = readFromProvider(preferredContext, packageName, allowDefault);
            if (configFromProvider != null) {
                return configFromProvider;
            }
        } catch (Throwable t) {
            XposedBridge.log("SpoofMyDevice: readFromProvider failed: " + t.getMessage());
        }

        return null;
    }

    private static RuntimeConfigStore.Snapshot readFromXSharedPrefs(String packageName, boolean allowDefault) {
        // Candidate 1: standard module preferences
        try {
            XSharedPreferences xsp = new XSharedPreferences("com.spoofmydevice", "app_profiles");
            RuntimeConfigStore.Snapshot result = extractFromXsp(xsp, packageName, allowDefault, "app_profiles");
            if (result != null) {
                return result;
            }
        } catch (Throwable t) {
            XposedBridge.log("SpoofMyDevice: XSP app_profiles error: " + t);
        }

        // Candidate 2: default preferences name
        try {
            XSharedPreferences xsp = new XSharedPreferences("com.spoofmydevice");
            RuntimeConfigStore.Snapshot result = extractFromXsp(xsp, packageName, allowDefault, "default");
            if (result != null) {
                return result;
            }
        } catch (Throwable t) {
            XposedBridge.log("SpoofMyDevice: XSP default error: " + t);
        }

        return null;
    }

    private static RuntimeConfigStore.Snapshot extractFromXsp(
        XSharedPreferences xsp, String packageName, boolean allowDefault, String label
    ) {
        RuntimeConfigStore.Snapshot result = ProfileConfigResolver.resolvePreferences(
            xsp.getAll(), packageName, allowDefault
        );
        if (result != null) {
            XposedBridge.log("SpoofMyDevice: Loaded profile state via XSharedPreferences ("
                + label + ") for " + packageName + ", assigned=" + result.hasAssignedProfile());
        }
        return result;
    }

    private static RuntimeConfigStore.Snapshot readFromDirectFiles(String packageName, boolean allowDefault) {
        String[] paths = new String[]{
            "/data/local/tmp/spoofmydevice/app_profiles.json",
            "/data/user_de/0/com.spoofmydevice/files/app_profiles.json",
            "/data/user/0/com.spoofmydevice/files/app_profiles.json",
            "/data/data/com.spoofmydevice/files/app_profiles.json"
        };
        for (String path : paths) {
            try {
                File file = new File(path);
                if (file.exists() && file.canRead() && file.length() <= MAX_PROFILE_BYTES) {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                        byte[] buffer = new byte[4096];
                        int count;
                        while ((count = fis.read(buffer)) != -1) {
                            if (bytes.size() + count > MAX_PROFILE_BYTES) {
                                throw new IllegalStateException("Profile state exceeds size limit");
                            }
                            bytes.write(buffer, 0, count);
                        }
                        RuntimeConfigStore.Snapshot parsed = ProfileConfigResolver.resolveJson(
                            new String(bytes.toByteArray(), StandardCharsets.UTF_8), packageName, allowDefault
                        );
                        if (parsed != null) {
                            XposedBridge.log("SpoofMyDevice: Loaded profile state from " + path + " for " + packageName);
                            return parsed;
                        }
                    }
                }
            } catch (Throwable t) {
                XposedBridge.log("SpoofMyDevice: Direct file error for " + path + ": " + t.getMessage());
            }
        }
        return null;
    }

    public static boolean isSystemPackage(String packageName) {
        try {
            if (HookSafetyPolicy.isProtectedUid(android.os.Process.myUid())) return true;
        } catch (Throwable ignored) {
            return true;
        }
        return HookSafetyPolicy.isProtectedPackage(packageName);
    }

    private static RuntimeConfigStore.Snapshot readFromProvider(
        Context preferredContext, String packageName, boolean allowDefault
    ) {
        if (preferredContext == null || isSystemPackage(packageName)) {
            return null;
        }
        try {
            Bundle bundle = preferredContext.getContentResolver().call(
                ConfigProvider.CONFIG_URI,
                ConfigProvider.METHOD_GET_STATE,
                packageName,
                null
            );
            if (bundle != null) {
                String content = bundle.getString(ConfigProvider.COLUMN_CONTENT);
                return ProfileConfigResolver.resolveJson(content, packageName, allowDefault);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Map<String, String> getEmbeddedDefaults() {
        Map<String, String> defaults = new HashMap<>();

        defaults.put("device.form_factor", "phone");
        defaults.put("device.has_telephony", "true");
        defaults.put("ro.product.brand", "google");
        defaults.put("ro.product.manufacturer", "Google");
        defaults.put("ro.product.model", "Pixel 7 Pro");
        defaults.put("ro.product.name", "cheetah");
        defaults.put("ro.product.device", "cheetah");
        defaults.put("ro.product.board", "cheetah");
        defaults.put("ro.hardware", "cheetah");
        defaults.put("ro.board.platform", "gs201");

        String[] partitions = {"product", "system", "system_ext", "vendor", "vendor_dlkm", "odm", "bootimage", "system_dlkm"};
        for (String partition : partitions) {
            defaults.put("ro.product." + partition + ".brand", "google");
            defaults.put("ro.product." + partition + ".manufacturer", "Google");
            defaults.put("ro.product." + partition + ".model", "Pixel 7 Pro");
            defaults.put("ro.product." + partition + ".name", "cheetah");
            defaults.put("ro.product." + partition + ".device", "cheetah");
        }

        defaults.put("ro.build.fingerprint", "google/cheetah/cheetah:15/AP4A.241205.013/12621605:user/release-keys");
        defaults.put("ro.build.id", "AP4A.241205.013");
        defaults.put("ro.build.display.id", "AP4A.241205.013");
        defaults.put("ro.build.version.incremental", "12621605");
        defaults.put("ro.build.type", "user");
        defaults.put("ro.build.tags", "release-keys");
        defaults.put("ro.build.description", "cheetah-user 15 AP4A.241205.013 12621605 release-keys");
        defaults.put("ro.build.product", "cheetah");
        defaults.put("ro.build.device", "cheetah");
        defaults.put("ro.build.characteristics", "nosdcard");
        defaults.put("ro.build.flavor", "cheetah-user");
        defaults.put("ro.build.version.release", "15");
        defaults.put("ro.build.version.release_or_codename", "15");
        defaults.put("ro.build.version.release_or_preview_display", "15");
        defaults.put("ro.build.version.sdk", "35");
        defaults.put("ro.build.version.codename", "REL");
        defaults.put("ro.build.version.security_patch", "2024-12-05");
        defaults.put("ro.product.build.fingerprint", "google/cheetah/cheetah:15/AP4A.241205.013/12621605:user/release-keys");
        defaults.put("ro.product.build.id", "AP4A.241205.013");
        defaults.put("ro.product.build.tags", "release-keys");
        defaults.put("ro.product.build.type", "user");
        defaults.put("ro.product.build.version.incremental", "12621605");
        defaults.put("ro.product.build.version.release", "15");
        defaults.put("ro.product.build.version.release_or_codename", "15");
        defaults.put("ro.product.build.version.sdk", "35");
        defaults.put("ro.system.build.fingerprint", "google/cheetah/cheetah:15/AP4A.241205.013/12621605:user/release-keys");
        defaults.put("ro.system_ext.build.fingerprint", "google/cheetah/cheetah:15/AP4A.241205.013/12621605:user/release-keys");
        defaults.put("ro.vendor.build.fingerprint", "google/cheetah/cheetah:15/AP4A.241205.013/12621605:user/release-keys");
        defaults.put("ro.odm.build.fingerprint", "google/cheetah/cheetah:15/AP4A.241205.013/12621605:user/release-keys");
        defaults.put("ro.bootimage.build.fingerprint", "google/cheetah/cheetah:15/AP4A.241205.013/12621605:user/release-keys");
        defaults.put("ro.system_dlkm.build.fingerprint", "google/cheetah/cheetah:15/AP4A.241205.013/12621605:user/release-keys");
        defaults.put("ro.vendor_dlkm.build.fingerprint", "google/cheetah/cheetah:15/AP4A.241205.013/12621605:user/release-keys");
        defaults.put("ro.vendor.build.version.release", "15");
        defaults.put("ro.vendor.build.version.release_or_codename", "15");
        defaults.put("ro.vendor_dlkm.build.version.release", "15");
        defaults.put("ro.vendor_dlkm.build.version.release_or_codename", "15");
        defaults.put("ro.odm.build.version.release", "15");
        defaults.put("ro.odm.build.version.release_or_codename", "15");
        defaults.put("ro.bootimage.build.version.release", "15");
        defaults.put("ro.bootimage.build.version.release_or_codename", "15");
        defaults.put("ro.system_dlkm.build.version.release", "15");
        defaults.put("ro.system_dlkm.build.version.release_or_codename", "15");

        defaults.put("ro.debuggable", "0");
        defaults.put("ro.secure", "1");
        defaults.put("ro.adb.secure", "1");
        defaults.put("ro.build.selinux", "0");
        defaults.put("ro.boot.verifiedbootstate", "green");
        defaults.put("ro.boot.flash.locked", "1");
        defaults.put("ro.boot.vbmeta.device_state", "locked");
        defaults.put("ro.boot.warranty_bit", "0");
        defaults.put("sys.oem_unlock_allowed", "0");
        defaults.put("ro.boot.veritymode", "enforcing");
        defaults.put("ro.crypto.state", "encrypted");
        defaults.put("ro.kernel.qemu", "0");
        defaults.put("ro.boot.qemu", "0");
        defaults.put("ro.boot.qemu.avd_name", "");
        defaults.put("ro.boot.qemu.camera_hq_edge_processing", "0");
        defaults.put("ro.boot.qemu.camera_protocol_ver", "0");
        defaults.put("ro.boot.qemu.cpuvulkan.version", "0");
        defaults.put("ro.boot.qemu.gltransport.drawFlushInterval", "0");
        defaults.put("ro.boot.qemu.gltransport.name", "");
        defaults.put("ro.boot.qemu.hwcodec.avcdec", "0");
        defaults.put("ro.boot.qemu.hwcodec.hevcdec", "0");
        defaults.put("ro.boot.qemu.hwcodec.vpxdec", "0");
        defaults.put("ro.boot.qemu.settings.system.screen_off_timeout", "0");
        defaults.put("ro.boot.qemu.virtiowifi", "0");
        defaults.put("ro.boot.qemu.vsync", "0");

        defaults.put("ro.boot.hardware", "cheetah");
        defaults.put("ro.boot.hardware.vulkan", "mali");
        defaults.put("ro.boot.hardware.gltransport", "");
        defaults.put("ro.boot.mode", "normal");
        defaults.put("ro.product.cpu.abi", "arm64-v8a");
        defaults.put("ro.product.cpu.abilist", "arm64-v8a,armeabi-v7a,armeabi");
        defaults.put("ro.product.cpu.abilist64", "arm64-v8a");
        defaults.put("ro.product.cpu.abilist32", "armeabi-v7a,armeabi");
        defaults.put("ro.arch", "arm64");
        defaults.put("ro.sf.lcd_density", "512");
        defaults.put("ro.treble.enabled", "true");
        defaults.put("ro.hardware.vulkan", "mali");
        defaults.put("ro.hardware.gralloc", "gs201");
        defaults.put("ro.hardware.power", "gs201-power");
        defaults.put("ro.hardware.egl", "mali");
        defaults.put("ro.soc.model", "gs201");
        defaults.put("ro.soc.manufacturer", "Google");
        defaults.put("screen.width", "1440");
        defaults.put("screen.height", "3120");
        defaults.put("screen.density", "512");
        defaults.put("dalvik.vm.heapsize", "576m");
        defaults.put("dalvik.vm.heapgrowthlimit", "256m");
        defaults.put("dalvik.vm.heapmaxfree", "8m");
        defaults.put("dalvik.vm.heapminfree", "512k");
        defaults.put("dalvik.vm.heapstartsize", "8m");
        defaults.put("dalvik.vm.heaptargetutilization", "0.75");

        defaults.put("gsm.operator.alpha", "T-Mobile");
        defaults.put("gsm.operator.numeric", "310260");
        defaults.put("gsm.sim.operator.alpha", "T-Mobile");
        defaults.put("gsm.sim.operator.numeric", "310260");
        defaults.put("gsm.sim.operator.iso-country", "us");
        defaults.put("persist.sys.timezone", "America/Los_Angeles");
        defaults.put("persist.sys.locale", "en-US");
        defaults.put("ro.product.locale", "en-US");
        defaults.put("ro.product.locale.language", "en");
        defaults.put("ro.product.locale.region", "US");
        defaults.put("persist.sys.usb.config", "none");
        defaults.put("webview.user_agent", "Mozilla/5.0 (Linux; Android 15; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36");

        return defaults;
    }

    private static String getConfigValue(String key) {
        return HookSafetyPolicy.isRuntimeStateProperty(key) ? null : STORE.current().properties.get(key);
    }

    private static boolean hasConfigValue(String key) {
        String value = getConfigValue(key);
        return value != null && !value.isEmpty();
    }

    private static String getOptionalConfigValue(String key) {
        String value = getConfigValue(key);
        if (value == null) {
            return null;
        }
        value = value.trim();
        return value.isEmpty() ? null : value;
    }

    private static String getFirstOptionalConfigValue(String... keys) {
        for (String key : keys) {
            String value = getOptionalConfigValue(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public static String getSystemProperty(String key, String defaultValue) {
        RuntimeConfigStore.Snapshot config = STORE.current();
        if (!config.hasAssignedProfile() || HookSafetyPolicy.isRuntimeStateProperty(key)) {
            return defaultValue;
        }
        String fieldId = getToggleFieldForSystemProperty(key);
        if (fieldId != null && !isSpoofEnabled(fieldId)) {
            return defaultValue;
        }
        String value = getConfigValue(key);
        if (value != null) {
            return value;
        }
        if (key.equals("gsm.version.baseband") || key.equals("gsm.version.baseband1") || key.equals("gsm.version.baseband2")) {
            return getBaseband();
        }
        if (key.equals("ro.baseband") || key.equals("ro.boot.baseband")) {
            return getBasebandType();
        }
        if (key.equals("ro.build.user")) {
            return getBuildUser();
        }
        if (key.equals("ro.build.host")) {
            return getBuildHost();
        }
        if (key.equals("ro.build.date.utc")) {
            return String.valueOf(getBuildTime() / 1000L);
        }
        if (key.equals("ro.build.date")) {
            return formatBuildDate(getBuildTime());
        }
        if (key.equals("ro.hardware.gps") || key.equals("ro.boot.hardware.gps")) {
            return getGnssHardwareModelName();
        }
        if (key.equals("ro.hardware.egl")) {
            return getHardwareEgl();
        }
        if (key.equals("ro.hardware.vulkan") || key.equals("ro.boot.hardware.vulkan")) {
            return getHardwareVulkan();
        }
        if (key.equals("ro.opengles.version")) {
            return getOpenGlesVersion();
        }
        if (key.equals("ro.cpuvulkan.version")) {
            return getCpuVulkanVersion();
        }
        if (key.equals("vendor.gpu.model")) {
            return getGpuRenderer();
        }
        if (key.equals("ro.soc.model")) {
            return getSocModel();
        }
        if (key.equals("ro.soc.manufacturer")) {
            return getSocManufacturer();
        }
        if (key.equals("ro.chipname") || key.equals("ro.hardware.chipname")) {
            return getBoardPlatform();
        }
        if (isHostLeakProperty(key)) {
            if (key.equals("ro.boot.product.hardware.sku") || key.equals("ro.boot.hardware.sku") || key.equals("ro.product.hardware.sku")) {
                String sku = getFirstOptionalConfigValue("ro.boot.hardware.sku", "device.sku");
                if (sku != null) return sku;
                String brand = getBuildBrand();
                String lowerBrand = brand != null ? brand.toLowerCase(Locale.US) : "";
                if (lowerBrand.contains("samsung")) return "SM-S938B";
                if (lowerBrand.contains("xiaomi")) return "xuanyuan";
                return "";
            }
            return "";
        }
        return defaultValue;
    }

    public static boolean isHostLeakProperty(String key) {
        if (!hasAssignedProfile() || key == null || key.isEmpty()
            || HookSafetyPolicy.isRuntimeStateProperty(key)) {
            return false;
        }
        String brand = getBuildBrand();
        String lowerBrand = brand != null ? brand.toLowerCase(Locale.US) : "";
        boolean isNonVivo = !lowerBrand.contains("vivo") && !lowerBrand.contains("iqoo");

        if (isNonVivo) {
            if (key.equals("vendor.gsm.project.baseband")
                || key.startsWith("vendor.gsm.project.")
                || key.equals("ro.boot.product.hardware.sku")
                || key.equals("ro.boot.hardware.sku")
                || key.equals("ro.product.hardware.sku")
                || key.equals("ro.hardware.bbk")
                || key.equals("ro.board.bbk")
                || key.equals("ro.build.version.bbk")
                || key.equals("ro.product.model.bbk")
                || key.equals("ro.product.customize.bbk")
                || key.equals("ro.product.net.entry.bbk")
                || key.equals("ro.vivo.product.platform")
                || key.equals("ro.vendor.soc.model")
                || key.equals("ro.vendor.soc.model.external_name")
                || key.equals("ro.vendor.soc.model.part_name")
                || key.startsWith("persist.vivo.")
                || key.startsWith("ro.vivo.")
                || key.equals("vendor.gpu.model")
                || key.startsWith("ro.vendor.gpu.")
                || key.startsWith("vendor.gpu.")) {
                return true;
            }
        }
        return false;
    }

    public static Map<String, String> getEffectiveSystemProperties() {
        RuntimeConfigStore.Snapshot config = STORE.current();
        Map<String, String> result = new LinkedHashMap<>();
        if (!config.hasAssignedProfile()) {
            return result;
        }
        for (Map.Entry<String, String> entry : config.properties.entrySet()) {
            String key = entry.getKey();
            if (!isShellVisibleSystemProperty(key)) {
                continue;
            }
            String toggleField = getToggleFieldForSystemProperty(key);
            if (toggleField != null && !isSpoofEnabled(toggleField)) {
                continue;
            }
            result.put(key, entry.getValue() == null ? "" : entry.getValue());
        }
        result.entrySet().removeIf(entry -> isHostLeakProperty(entry.getKey()));
        if (!result.containsKey("ro.boot.product.hardware.sku")) {
            String sku = getSystemProperty("ro.boot.product.hardware.sku", "");
            if (!sku.isEmpty()) {
                result.put("ro.boot.product.hardware.sku", sku);
            }
        }
        if (!result.containsKey("gsm.version.baseband")) {
            result.put("gsm.version.baseband", getBaseband());
        }
        if (!result.containsKey("ro.baseband")) {
            result.put("ro.baseband", getBasebandType());
        }
        if (!result.containsKey("ro.build.user")) {
            result.put("ro.build.user", getBuildUser());
        }
        if (!result.containsKey("ro.build.host")) {
            result.put("ro.build.host", getBuildHost());
        }
        if (!result.containsKey("ro.build.date.utc")) {
            result.put("ro.build.date.utc", String.valueOf(getBuildTime() / 1000L));
        }
        if (!result.containsKey("ro.build.date")) {
            result.put("ro.build.date", formatBuildDate(getBuildTime()));
        }
        if (!result.containsKey("ro.hardware.gps")) {
            result.put("ro.hardware.gps", getGnssHardwareModelName());
        }
        return result;
    }

    private static boolean isShellVisibleSystemProperty(String key) {
        if (key == null || key.trim().isEmpty() || HookSafetyPolicy.isRuntimeStateProperty(key)) {
            return false;
        }
        if (key.startsWith("device.")) {
            return false;
        }
        if (key.equals(KEY_APPLY_SCREEN_METRICS)
            || key.equals(KEY_SPOOF_IMEI)
            || key.equals(KEY_SPOOF_MEID)
            || key.equals(KEY_SPOOF_IMSI)
            || key.equals(KEY_SPOOF_ICCID)
            || key.equals(KEY_SPOOF_PHONE_NUMBER)
            || key.equals(KEY_SPOOF_GAID)
            || key.equals(KEY_SPOOF_GSF_ID)
            || key.equals(KEY_SPOOF_MEDIA_DRM_ID)
            || key.equals(KEY_SPOOF_APP_SET_ID)
            || key.equals(KEY_SAFE_MODE_PACKAGES)
            || key.startsWith(KEY_SPOOF_TOGGLE_PREFIX)
            || key.equals("ANDROID_ID")) {
            return false;
        }
        return true;
    }

    public static boolean isTabletProfile() {
        String formFactor = getConfigValue("device.form_factor");
        if (formFactor != null && !formFactor.isEmpty()) {
            return "tablet".equalsIgnoreCase(formFactor);
        }

        String characteristics = getBuildCharacteristics();
        if (characteristics != null && characteristics.toLowerCase(Locale.US).contains("tablet")) {
            return true;
        }

        return getSmallestScreenWidthDp() >= 600;
    }

    public static boolean hasTelephonySupport() {
        String explicit = getConfigValue("device.has_telephony");
        if (explicit != null && !explicit.isEmpty()) {
            return explicit.equals("1") || explicit.equalsIgnoreCase("true");
        }
        return !isTabletProfile();
    }

    public static boolean shouldExposeTelephony() {
        return hasTelephonySupport() || shouldReportSimPresent();
    }

    public static boolean shouldReportSimPresent() {
        return getICCID() != null
            || getIMSI() != null
            || getPhoneNumber() != null
            || getSystemProperty("gsm.sim.operator.alpha", null) != null
            || getSystemProperty("gsm.sim.operator.numeric", null) != null
            || getSystemProperty("gsm.sim.operator.iso-country", null) != null
            || getSystemProperty("gsm.operator.alpha", null) != null
            || getSystemProperty("gsm.operator.numeric", null) != null;
    }

    public static int getScreenWidth() {
        return parseInt(getConfigValue("screen.width"), 1440);
    }

    public static int getScreenHeight() {
        return parseInt(getConfigValue("screen.height"), 3120);
    }

    public static int getScreenDensityDpi() {
        return parseInt(getConfigValue("screen.density"), parseInt(getConfigValue("ro.sf.lcd_density"), 480));
    }

    public static int getSmallestScreenWidthDp() {
        int width = getScreenWidth();
        int height = getScreenHeight();
        int density = getScreenDensityDpi();
        if (density <= 0) {
            density = 480;
        }
        return Math.round((Math.min(width, height) * 160f) / density);
    }

    public static int getScreenWidthDp() {
        int density = getScreenDensityDpi();
        if (density <= 0) {
            density = 480;
        }
        return Math.round((getScreenWidth() * 160f) / density);
    }

    public static int getScreenHeightDp() {
        int density = getScreenDensityDpi();
        if (density <= 0) {
            density = 480;
        }
        return Math.round((getScreenHeight() * 160f) / density);
    }

    public static float getScreenDensity() {
        return getScreenDensityDpi() / 160f;
    }

    public static boolean shouldApplyScreenMetrics() {
        String explicit = getConfigValue(KEY_APPLY_SCREEN_METRICS);
        if (explicit == null || explicit.isEmpty()) {
            return false;
        }
        return explicit.equals("1") || explicit.equalsIgnoreCase("true");
    }

    public static boolean shouldBypassVersionSpoof(String packageName) {
        if (packageName == null || packageName.trim().isEmpty()) {
            return false;
        }
        String configured = getOptionalConfigValue(KEY_SAFE_MODE_PACKAGES);
        if (configured == null) {
            return false;
        }
        String normalizedPackage = packageName.trim();
        String[] parts = configured.split("[,\\n]");
        for (String part : parts) {
            if (normalizedPackage.equals(part.trim())) {
                return true;
            }
        }
        return false;
    }

    public static String getTogglePropertyKey(String fieldId) {
        return KEY_SPOOF_TOGGLE_PREFIX + fieldId;
    }

    public static boolean isSpoofEnabled(String fieldId) {
        if (!hasAssignedProfile()) {
            return false;
        }
        if (fieldId == null || fieldId.trim().isEmpty()) {
            return true;
        }
        String explicit = getOptionalConfigValue(getTogglePropertyKey(fieldId));
        if (explicit == null) {
            return true;
        }
        return explicit.equals("1") || explicit.equalsIgnoreCase("true");
    }

    public static String getToggleFieldForSystemProperty(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        if (key.equals("ro.product.brand") || key.endsWith(".brand")) {
            return FIELD_BRAND;
        }
        if (key.equals("ro.product.manufacturer") || key.endsWith(".manufacturer")) {
            return FIELD_MANUFACTURER;
        }
        if (key.equals("ro.product.model") || key.endsWith(".model")) {
            return FIELD_MODEL;
        }
        if (key.equals("ro.product.device") || key.equals("ro.build.device") || key.endsWith(".device")) {
            return FIELD_DEVICE;
        }
        if (key.equals("ro.product.name") || key.equals("ro.build.product") || key.endsWith(".name")) {
            return FIELD_PRODUCT;
        }
        if (key.equals("ro.product.board")) {
            return FIELD_BOARD;
        }
        if (key.equals("ro.hardware") || key.equals("ro.boot.hardware")) {
            return FIELD_HARDWARE;
        }
        if (key.equals("ro.board.platform")) {
            return FIELD_BOARD_PLATFORM;
        }
        if (key.equals("ro.build.display.id")) {
            return FIELD_BUILD_DISPLAY_ID;
        }
        if (key.equals("ro.build.id") || key.equals("ro.product.build.id")) {
            return FIELD_BUILD_ID;
        }
        if (key.equals("ro.build.version.incremental") || key.equals("ro.product.build.version.incremental")) {
            return FIELD_BUILD_INCREMENTAL;
        }
        if (key.equals("ro.build.version.release")
            || key.equals("ro.build.version.release_or_codename")
            || key.equals("ro.build.version.release_or_preview_display")
            || key.equals("ro.product.build.version.release")
            || key.equals("ro.product.build.version.release_or_codename")
            || key.endsWith(".build.version.release")
            || key.endsWith(".build.version.release_or_codename")) {
            return FIELD_ANDROID_RELEASE;
        }
        if (key.equals("ro.build.version.sdk")
            || key.equals("ro.product.build.version.sdk")
            || key.endsWith(".build.version.sdk")) {
            return FIELD_SDK;
        }
        if (key.equals("ro.build.version.security_patch")) {
            return FIELD_SECURITY_PATCH;
        }
        if (key.equals("ro.build.fingerprint")
            || key.equals("ro.product.build.fingerprint")
            || key.endsWith(".build.fingerprint")) {
            return FIELD_FINGERPRINT;
        }
        if (key.equals("screen.width")) {
            return FIELD_SCREEN_WIDTH;
        }
        if (key.equals("screen.height")) {
            return FIELD_SCREEN_HEIGHT;
        }
        if (key.equals("screen.density") || key.equals("ro.sf.lcd_density")) {
            return FIELD_SCREEN_DENSITY;
        }
        if (key.equals("gsm.operator.alpha") || key.equals("gsm.sim.operator.alpha")) {
            return FIELD_OPERATOR_ALPHA;
        }
        if (key.equals("gsm.operator.numeric") || key.equals("gsm.sim.operator.numeric")) {
            return FIELD_OPERATOR_NUMERIC;
        }
        if (key.equals("gsm.sim.operator.iso-country")) {
            return FIELD_SIM_COUNTRY;
        }
        if (key.equals("persist.sys.timezone")) {
            return FIELD_TIMEZONE;
        }
        if (key.equals("persist.sys.locale")
            || key.equals("ro.product.locale")
            || key.equals("ro.product.locale.language")
            || key.equals("ro.product.locale.region")) {
            return FIELD_LOCALE;
        }
        if (key.equals("gsm.version.baseband") || key.equals("ro.baseband")) {
            return FIELD_BASEBAND;
        }
        if (key.equals("ro.build.user")) {
            return FIELD_BUILD_USER;
        }
        if (key.equals("ro.build.host")) {
            return FIELD_BUILD_HOST;
        }
        if (key.equals("ro.bootloader")) {
            return FIELD_BOOTLOADER;
        }
        if (key.equals("ro.soc.model")) {
            return FIELD_SOC_MODEL;
        }
        if (key.equals("ro.soc.manufacturer")) {
            return FIELD_SOC_MANUFACTURER;
        }
        if (key.equals("ro.build.date.utc")) {
            return FIELD_BUILD_DATE_UTC;
        }
        if (key.equals("ro.build.date")) {
            return FIELD_BUILD_DATE;
        }
        if (key.equals("device.gnss_model") || key.equals("ro.hardware.gps")) {
            return FIELD_GNSS_MODEL;
        }
        if (key.equals("device.gnss_year")) {
            return FIELD_GNSS_YEAR;
        }
        if (key.equals("ro.build.description")) {
            return FIELD_BUILD_DESCRIPTION;
        }
        if (key.equals("ro.build.flavor")) {
            return FIELD_BUILD_FLAVOR;
        }
        if (key.equals("ro.build.product")) {
            return FIELD_BUILD_PRODUCT;
        }
        if (key.equals("ro.build.characteristics")) {
            return FIELD_BUILD_CHARACTERISTICS;
        }
        if (key.equals("ro.serialno") || key.equals("ro.boot.serialno")) {
            return FIELD_SERIAL_NUMBER;
        }
        if (key.equals("ro.product.cpu.abi")) {
            return FIELD_CPU_ABI;
        }
        if (key.equals("ro.product.cpu.abilist")) {
            return FIELD_CPU_ABI_LIST;
        }
        if (key.equals("ro.product.cpu.abilist64")) {
            return FIELD_CPU_ABI_LIST_64;
        }
        if (key.equals("ro.product.cpu.abilist32")) {
            return FIELD_CPU_ABI_LIST_32;
        }
        if (key.equals("webview.user_agent")) {
            return FIELD_USER_AGENT;
        }
        if (key.equals("gsm.sim.operator.alpha")) {
            return FIELD_SIM_OPERATOR_ALPHA;
        }
        if (key.equals("gsm.sim.operator.numeric")) {
            return FIELD_SIM_OPERATOR_NUMERIC;
        }
        if (key.equals("gpu.vendor")) {
            return FIELD_GPU_VENDOR;
        }
        if (key.equals("gpu.renderer") || key.equals("vendor.gpu.model")) {
            return FIELD_GPU_RENDERER;
        }
        if (key.equals("gl.version")) {
            return FIELD_GL_VERSION;
        }
        if (key.equals("gl.sl_version")) {
            return FIELD_GLSL_VERSION;
        }
        if (key.equals("vulkan.version")) {
            return FIELD_VULKAN_VERSION;
        }
        if (key.equals("vulkan.driver_version")) {
            return FIELD_VULKAN_DRIVER_VERSION;
        }
        if (key.equals("vulkan.level")) {
            return FIELD_VULKAN_LEVEL;
        }
        if (key.equals("cpu.hardware")) {
            return FIELD_CPU_HARDWARE;
        }
        if (key.equals("cpu.cores")) {
            return FIELD_CPU_CORES;
        }
        return null;
    }

    public static String getTimezone() {
        if (!isSpoofEnabled(FIELD_TIMEZONE)) {
            return null;
        }
        return getOptionalConfigValue("persist.sys.timezone");
    }

    public static String getLocaleTag() {
        if (!isSpoofEnabled(FIELD_LOCALE)) {
            return null;
        }
        return getFirstOptionalConfigValue("persist.sys.locale", "ro.product.locale");
    }

    public static String getIMEI() {
        if (!isSpoofEnabled(FIELD_IMEI)) {
            return null;
        }
        return getOptionalConfigValue(KEY_SPOOF_IMEI);
    }

    public static String getMEID() {
        if (!isSpoofEnabled(FIELD_MEID)) {
            return null;
        }
        return getOptionalConfigValue(KEY_SPOOF_MEID);
    }

    public static String getIMSI() {
        if (!isSpoofEnabled(FIELD_IMSI)) {
            return null;
        }
        return getOptionalConfigValue(KEY_SPOOF_IMSI);
    }

    public static String getICCID() {
        if (!isSpoofEnabled(FIELD_ICCID)) {
            return null;
        }
        return getOptionalConfigValue(KEY_SPOOF_ICCID);
    }

    public static String getPhoneNumber() {
        if (!isSpoofEnabled(FIELD_PHONE_NUMBER)) {
            return null;
        }
        return getOptionalConfigValue(KEY_SPOOF_PHONE_NUMBER);
    }

    public static String getSerial() {
        return getFirstOptionalConfigValue("ro.serialno", "ro.boot.serialno", "SERIAL_NUMBER");
    }

    public static String getGAID() {
        if (!isSpoofEnabled(FIELD_GAID)) {
            return null;
        }
        return getOptionalConfigValue(KEY_SPOOF_GAID);
    }

    public static String getGSFId() {
        if (!isSpoofEnabled(FIELD_GSF_ID)) {
            return null;
        }
        return getOptionalConfigValue(KEY_SPOOF_GSF_ID);
    }

    public static String getAndroidId() {
        if (!isSpoofEnabled(FIELD_ANDROID_ID)) {
            return null;
        }
        return getOptionalConfigValue("ANDROID_ID");
    }

    public static byte[] getMediaDrmId() {
        if (!isSpoofEnabled(FIELD_MEDIA_DRM_ID)) {
            return null;
        }
        String hex = getOptionalConfigValue(KEY_SPOOF_MEDIA_DRM_ID);
        if (hex == null) {
            return null;
        }
        return decodeHex(hex);
    }

    public static String getAppSetId() {
        if (!isSpoofEnabled(FIELD_APP_SET_ID)) {
            return null;
        }
        return getOptionalConfigValue(KEY_SPOOF_APP_SET_ID);
    }

    public static boolean isConfigAvailable() {
        return hasAssignedProfile();
    }

    public static String getBuildFingerprint() {
        return getConfigValue("ro.build.fingerprint");
    }

    public static String getBuildModel() {
        return getConfigValue("ro.product.model");
    }

    public static String getBuildDevice() {
        return getConfigValue("ro.product.device");
    }

    public static String getBuildManufacturer() {
        return getConfigValue("ro.product.manufacturer");
    }

    public static String getBuildBrand() {
        return getConfigValue("ro.product.brand");
    }

    public static String getBuildProduct() {
        return getConfigValue("ro.product.name");
    }

    public static String getBuildBoard() {
        return getConfigValue("ro.product.board");
    }

    public static String getBoardPlatform() {
        String platform = getConfigValue("ro.board.platform");
        if (platform != null && !platform.isEmpty()) {
            return platform;
        }
        return getBuildBoard();
    }

    public static String getBuildHardware() {
        return getConfigValue("ro.hardware");
    }

    public static String getBuildBootloader() {
        return getOptionalConfigValue("ro.bootloader");
    }

    public static String getBuildId() {
        return getConfigValue("ro.build.id");
    }

    public static String getBuildDisplay() {
        return getConfigValue("ro.build.display.id");
    }

    public static String getBuildTags() {
        return getConfigValue("ro.build.tags");
    }

    public static String getBuildType() {
        return getConfigValue("ro.build.type");
    }

    public static String getBuildVersionRelease() {
        return getConfigValue("ro.build.version.release");
    }

    public static int getBuildVersionSdk() {
        return parseInt(getConfigValue("ro.build.version.sdk"), 35);
    }

    public static String getBuildVersionSecurityPatch() {
        return getConfigValue("ro.build.version.security_patch");
    }

    public static String getBuildVersionIncremental() {
        return getConfigValue("ro.build.version.incremental");
    }

    public static String getBuildVersionCodename() {
        return getConfigValue("ro.build.version.codename");
    }

    public static String getBuildDescription() {
        return getConfigValue("ro.build.description");
    }

    public static String getBuildCharacteristics() {
        return getConfigValue("ro.build.characteristics");
    }

    public static String getBuildFlavor() {
        return getConfigValue("ro.build.flavor");
    }

    public static String getWebViewUserAgent() {
        String ua = getConfigValue("webview.user_agent");
        String targetModel = getBuildModel();
        String targetRelease = getBuildVersionRelease();
        if (targetRelease == null || targetRelease.isEmpty()) {
            targetRelease = "15";
        }
        if (targetModel == null || targetModel.isEmpty()) {
            targetModel = "Android";
        }

        // If the stored UA in json still contains host device strings (e.g. V2507A / PD2507 / vivo), purge it
        if (ua != null && !ua.trim().isEmpty()) {
            String lowerBrand = (getBuildBrand() != null ? getBuildBrand() : "").toLowerCase(Locale.US);
            if (!lowerBrand.contains("vivo") && (ua.contains("V2507A") || ua.contains("PD2507") || ua.toLowerCase(Locale.US).contains("vivo"))) {
                ua = null;
            }
        }

        if (ua != null && !ua.trim().isEmpty()) {
            return ua;
        }

        String buildId = getBuildId();
        if (buildId != null && !buildId.isEmpty()) {
            return String.format(
                Locale.US,
                "Mozilla/5.0 (Linux; Android %s; %s Build/%s; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/131.0.0.0 Mobile Safari/537.36",
                targetRelease,
                targetModel,
                buildId
            );
        }
        return String.format(
            Locale.US,
            "Mozilla/5.0 (Linux; Android %s; %s) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
            targetRelease,
            targetModel
        );
    }

    public static String getCpuAbi() {
        return getConfigValue("ro.product.cpu.abi");
    }

    public static String getCpuAbiList() {
        return getConfigValue("ro.product.cpu.abilist");
    }

    public static String getCpuAbiList64() {
        return getConfigValue("ro.product.cpu.abilist64");
    }

    public static String getCpuAbiList32() {
        return getConfigValue("ro.product.cpu.abilist32");
    }

    public static String getSocModel() {
        String soc = getConfigValue("ro.soc.model");
        if (soc != null && !soc.isEmpty()) {
            return soc;
        }
        String board = getBuildBoard();
        if (board != null && board.contains("sun")) {
            return "Snapdragon 8 Elite";
        }
        return "";
    }

    public static String getSocManufacturer() {
        String mfg = getConfigValue("ro.soc.manufacturer");
        if (mfg != null && !mfg.isEmpty()) {
            return mfg;
        }
        String hw = getBuildHardware();
        if (hw != null && (hw.contains("qcom") || hw.contains("qualcomm"))) {
            return "Qualcomm";
        }
        return "";
    }

    public static String getOdmSku() {
        String sku = getFirstOptionalConfigValue("ro.boot.product.hardware.sku", "device.odm_sku");
        if (sku != null) {
            return sku;
        }
        return "";
    }

    public static String getSku() {
        String sku = getFirstOptionalConfigValue("ro.boot.hardware.sku", "device.sku");
        if (sku != null) {
            return sku;
        }
        return "";
    }

    public static String getBaseband() {
        if (!isSpoofEnabled(FIELD_BASEBAND)) {
            return null;
        }
        String baseband = getFirstOptionalConfigValue("gsm.version.baseband", "device.baseband");
        if (baseband != null) {
            return baseband;
        }
        String brand = getBuildBrand();
        String board = getBuildBoard();
        String hardware = getBuildHardware();
        String incremental = getBuildVersionIncremental();
        if (incremental == null) {
            incremental = "001";
        }

        String lowerBrand = brand != null ? brand.toLowerCase(Locale.US) : "";
        String lowerBoard = board != null ? board.toLowerCase(Locale.US) : "";
        String lowerHw = hardware != null ? hardware.toLowerCase(Locale.US) : "";

        boolean isQualcomm = lowerBoard.contains("sun") || lowerBoard.contains("kalama")
            || lowerBoard.contains("taro") || lowerBoard.contains("lahaina")
            || lowerHw.contains("qcom") || lowerHw.contains("qualcomm");

        if (lowerBrand.contains("xiaomi")) {
            if (isQualcomm) {
                return "MPSS.DE.1.0-" + incremental + "-SUN_GEN_PACK-1";
            }
            return "MPSS.DE.1.0-" + incremental;
        } else if (lowerBrand.contains("samsung")) {
            String inc = getBuildVersionIncremental();
            if (inc != null && inc.length() >= 8) {
                int underscore = inc.indexOf('_');
                return underscore > 0 ? inc.substring(0, underscore) : inc;
            }
            String dev = getBuildDevice();
            if ("pa3q".equalsIgnoreCase(dev)) {
                return "S938BXXU0AXKG";
            }
            if ("gts8uwifi".equalsIgnoreCase(dev)) {
                return "";
            }
            return "S938BXXU0AXKG";
        } else if (lowerBrand.contains("google")) {
            return "g5300g-240913-241021-B-12543976";
        } else if (isQualcomm) {
            return "MPSS.DE.1.0-" + incremental;
        }
        return "MPSS.DE.1.0-" + incremental;
    }

    public static String getBasebandType() {
        String basebandType = getFirstOptionalConfigValue("ro.baseband", "ro.boot.baseband");
        if (basebandType != null) {
            return basebandType;
        }
        String board = getBuildBoard();
        String hw = getBuildHardware();
        String lowerBoard = board != null ? board.toLowerCase(Locale.US) : "";
        String lowerHw = hw != null ? hw.toLowerCase(Locale.US) : "";
        if (lowerBoard.contains("sun") || lowerHw.contains("qcom") || lowerHw.contains("qualcomm")) {
            return "qcom";
        }
        return "qcom";
    }

    public static String getBuildUser() {
        String user = getFirstOptionalConfigValue("ro.build.user", "device.build_user");
        if (user != null) {
            return user;
        }
        String brand = getBuildBrand();
        if (brand != null) {
            String lower = brand.toLowerCase(Locale.US);
            if (lower.contains("xiaomi") || lower.contains("redmi") || lower.contains("poco")) {
                return "builder";
            }
            if (lower.contains("google")) {
                return "android-build";
            }
            if (lower.contains("samsung")) {
                return "dpi";
            }
        }
        return "android-build";
    }

    public static String getBuildHost() {
        String host = getFirstOptionalConfigValue("ro.build.host", "device.build_host");
        if (host != null) {
            return host;
        }
        String brand = getBuildBrand();
        if (brand != null) {
            String lower = brand.toLowerCase(Locale.US);
            if (lower.contains("xiaomi") || lower.contains("redmi") || lower.contains("poco")) {
                return "c5-miui-ota-bd128.bj";
            }
            if (lower.contains("google")) {
                return "google.com";
            }
            if (lower.contains("samsung")) {
                return "SWDD5712";
            }
        }
        return "buildhost.local";
    }

    public static long getBuildTime() {
        String utcStr = getFirstOptionalConfigValue("ro.build.date.utc", "device.build_time");
        if (utcStr != null) {
            try {
                long parsed = Long.parseLong(utcStr);
                return parsed < 10000000000L ? parsed * 1000L : parsed;
            } catch (Throwable ignored) {
            }
        }
        String patch = getBuildVersionSecurityPatch();
        if (patch != null && patch.length() >= 10) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = sdf.parse(patch);
                if (date != null) {
                    return date.getTime() + 12L * 3600L * 1000L;
                }
            } catch (Throwable ignored) {
            }
        }
        return 1735689600000L;
    }

    public static String formatBuildDate(long timeMs) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.format(new Date(timeMs));
        } catch (Throwable ignored) {
            return "Wed Jan 1 12:00:00 UTC 2025";
        }
    }

    public static String getGnssHardwareModelName() {
        String gnss = getFirstOptionalConfigValue("ro.hardware.gps", "device.gnss_model");
        if (gnss != null) {
            return gnss;
        }
        return "Qualcomm GNSS";
    }

    public static int getGnssYearOfHardware() {
        String yearStr = getOptionalConfigValue("device.gnss_year");
        if (yearStr != null) {
            int year = parseInt(yearStr, 0);
            if (year > 0) {
                return year;
            }
        }
        String patch = getBuildVersionSecurityPatch();
        if (patch != null && patch.length() >= 4) {
            try {
                int y = Integer.parseInt(patch.substring(0, 4));
                if (y >= 2018 && y <= 2030) {
                    return y;
                }
            } catch (Throwable ignored) {
            }
        }
        return 2025;
    }

    public static boolean isTensorG6Device() {
        String platform = getBoardPlatform().toLowerCase(Locale.US);
        String board = getBuildBoard().toLowerCase(Locale.US);
        return platform.contains("malibu") || board.contains("cubs") || board.contains("stallion");
    }

    public static String getGpuVendor() {
        if (!isSpoofEnabled(FIELD_GPU_VENDOR)) {
            return null;
        }
        String vendor = getFirstOptionalConfigValue("gpu.vendor");
        if (vendor != null && !vendor.isEmpty()) {
            return vendor;
        }
        if (isTensorG6Device()) {
            return "Imagination Technologies";
        }
        return isGoogleTensorDevice() ? "ARM" : "Qualcomm";
    }

    public static String getGpuRenderer() {
        if (!isSpoofEnabled(FIELD_GPU_RENDERER)) {
            return null;
        }
        String renderer = getFirstOptionalConfigValue("gpu.renderer", "vendor.gpu.model");
        if (renderer != null && !renderer.isEmpty()) {
            return renderer;
        }
        if (isTensorG6Device()) {
            return "PowerVR CXTP-48-1536";
        }
        return isGoogleTensorDevice() ? "Mali-G715-Immortalis MC10" : "Adreno (TM) 830";
    }

    public static String getGlVersion() {
        if (!isSpoofEnabled(FIELD_GL_VERSION)) {
            return null;
        }
        String gl = getFirstOptionalConfigValue("gl.version");
        if (gl != null && !gl.isEmpty()) {
            return gl;
        }
        if (isTensorG6Device()) {
            return "OpenGL ES 3.2 (PowerVR Software v26.1)";
        }
        return isGoogleTensorDevice()
            ? "OpenGL ES 3.2 v1.r44p0-01eac0"
            : "OpenGL ES 3.2 V@0797.0 (GIT@e7ff6ef885, I677ff0eb96, 1729000109) (Date:10/15/24)";
    }

    public static String getGlslVersion() {
        if (!isSpoofEnabled(FIELD_GLSL_VERSION)) {
            return null;
        }
        String glsl = getFirstOptionalConfigValue("gl.sl_version");
        if (glsl != null && !glsl.isEmpty()) {
            return glsl;
        }
        return "OpenGL ES GLSL ES 3.20";
    }

    public static String getVulkanVersion() {
        if (!isSpoofEnabled(FIELD_VULKAN_VERSION)) {
            return null;
        }
        String vk = getFirstOptionalConfigValue("vulkan.version");
        if (vk != null && !vk.isEmpty()) {
            return vk;
        }
        return isGoogleTensorDevice() ? "1.3.268" : "1.3.296";
    }

    public static int getVulkanVersionCode() {
        String vk = getVulkanVersion();
        if (vk == null || vk.isEmpty()) {
            return 0x403000;
        }
        try {
            String[] parts = vk.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return (major << 22) | (minor << 12) | patch;
        } catch (Throwable t) {
            return 0x403000;
        }
    }

    public static int getGlEsVersionCode() {
        return 0x00030002;
    }

    public static int getVulkanLevel() {
        if (!isSpoofEnabled(FIELD_VULKAN_LEVEL)) {
            return 1;
        }
        return parseInt(getFirstOptionalConfigValue("vulkan.level"), 1);
    }

    public static String getVulkanDriverVersion() {
        if (!isSpoofEnabled(FIELD_VULKAN_DRIVER_VERSION)) {
            return null;
        }
        String drv = getFirstOptionalConfigValue("vulkan.driver_version");
        if (drv != null && !drv.isEmpty()) {
            return drv;
        }
        return isGoogleTensorDevice() ? "44.0.0" : "512.797.0";
    }

    public static String getCpuHardware() {
        if (!isSpoofEnabled(FIELD_CPU_HARDWARE)) {
            return null;
        }
        String cpuHw = getFirstOptionalConfigValue("cpu.hardware");
        if (cpuHw != null && !cpuHw.isEmpty()) {
            return cpuHw;
        }
        if (isGoogleTensorDevice()) {
            return "Google Tensor G3";
        }
        return "Qualcomm Technologies, Inc SM8750";
    }

    public static int getCpuCores() {
        if (!isSpoofEnabled(FIELD_CPU_CORES)) {
            return 8;
        }
        String coresStr = getFirstOptionalConfigValue("cpu.cores");
        if (coresStr != null && !coresStr.isEmpty()) {
            return parseInt(coresStr, 8);
        }
        if (isTensorG6Device()) {
            return 7;
        }
        return isGoogleTensorDevice() ? 9 : 8;
    }

    public static String getHardwareEgl() {
        if (isTensorG6Device()) {
            return "powervr";
        }
        return isGoogleTensorDevice() ? "mali" : "adreno";
    }

    public static String getHardwareVulkan() {
        if (isTensorG6Device()) {
            return "powervr";
        }
        return isGoogleTensorDevice() ? "mali" : "adreno";
    }

    public static String getOpenGlesVersion() {
        return "196610";
    }

    public static String getCpuVulkanVersion() {
        return "4206592";
    }

    public static boolean isGoogleTensorDevice() {
        String mfg = getSocManufacturer().toLowerCase(Locale.US);
        String board = getBuildBoard().toLowerCase(Locale.US);
        String platform = getBoardPlatform().toLowerCase(Locale.US);
        return mfg.contains("google") || board.contains("zuma") || board.contains("husky")
            || board.contains("cheetah") || platform.contains("zuma") || platform.contains("gs201")
            || board.contains("cubs") || board.contains("stallion") || platform.contains("malibu") || platform.contains("laguna");
    }

    public static String getCacheL3Size() {
        String model = getSocModel().toLowerCase(Locale.US);
        if (model.contains("elite") || model.contains("sm8750")) {
            return "16384K";
        }
        if (model.contains("tensor g6") || isTensorG6Device()) {
            return "16384K";
        }
        if (model.contains("tensor g5")) {
            return "12288K";
        }
        if (model.contains("tensor")) {
            return "8192K";
        }
        if (model.contains("gen 1") || model.contains("sm8450")) {
            return "6144K";
        }
        return "16384K";
    }

    public static String getCpuMaxFreq() {
        String model = getSocModel().toLowerCase(Locale.US);
        if (model.contains("elite") || model.contains("sm8750")) {
            return "4320000";
        }
        if (model.contains("tensor g6") || isTensorG6Device()) {
            return "4110000";
        }
        if (model.contains("tensor g5")) {
            return "3400000";
        }
        if (model.contains("tensor g4")) {
            return "3100000";
        }
        if (model.contains("tensor g3")) {
            return "2910000";
        }
        if (model.contains("tensor g2")) {
            return "2850000";
        }
        return "3200000";
    }

    public static String getCpuMinFreq() {
        return "300000";
    }

    public static String getCpuInfoContent() {
        int cores = getCpuCores();
        String cpuHw = getCpuHardware();
        boolean isTensor = isGoogleTensorDevice();
        String implementer = isTensor ? "0x41" : "0x51";
        String part = isTensor ? "0xd46" : "0x001";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cores; i++) {
            sb.append("processor\t: ").append(i).append("\n");
            sb.append("BogoMIPS\t: ").append(isTensor ? "26.00" : "38.40").append("\n");
            sb.append("Features\t: fp asimd evtstrm aes pmull sha1 sha2 crc32 atomics fphp asimdhp cpuid asimdrdm jscvt fcma lrcpc dcpop sha3 asimddp sha512 sve sve2\n");
            sb.append("CPU implementer\t: ").append(implementer).append("\n");
            sb.append("CPU architecture: 8\n");
            sb.append("CPU variant\t: 0x0\n");
            sb.append("CPU part\t: ").append(part).append("\n");
            sb.append("CPU revision\t: 0\n\n");
        }
        sb.append("Hardware\t: ").append(cpuHw).append("\n");
        return sb.toString();
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static byte[] decodeHex(String hex) {
        String normalized = hex.replace(" ", "").replace(":", "");
        if ((normalized.length() % 2) != 0) {
            return null;
        }

        try {
            byte[] result = new byte[normalized.length() / 2];
            for (int index = 0; index < normalized.length(); index += 2) {
                int value = Integer.parseInt(normalized.substring(index, index + 2), 16);
                result[index / 2] = (byte) value;
            }
            return result;
        } catch (Exception ignored) {
            return null;
        }
    }

}

