package com.devicespooflab.hooks.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Immutable, process-local identity data shared by Java, native and shell readers. */
public final class HardwareIdentity {
    private final Map<String, String> values;
    public final boolean active;
    public final int cores;
    public final long ramBytes;
    private final int[] frequencies;
    private final int[] parts;
    private final Map<String, String> fileView;
    private final Map<String, List<String>> directoryView;

    public HardwareIdentity(Map<String, String> properties, boolean active) {
        values = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
        this.active = active;
        String soc = value("ro.soc.model", value("cpu.hardware", "ARM"));
        String lower = soc.toLowerCase(Locale.ROOT);
        int fallbackCores = lower.contains("tensor g3") ? 9 : lower.contains("tensor g6") ? 7 : 8;
        cores = Math.max(1, Math.min(32, integer("cpu.cores", fallbackCores)));
        frequencies = new int[cores];
        parts = new int[cores];
        Arrays.fill(frequencies, 2800000);
        Arrays.fill(parts, 0xd41);
        if (lower.contains("tensor g5")) {
            setCluster(0, 2, 2250000, 0xd80);
            setCluster(2, 7, 3050000, 0xd87);
            setCluster(7, cores, 3780000, 0xd82);
        } else if (lower.contains("tensor g4")) {
            setCluster(0, 4, 1950000, 0xd80);
            setCluster(4, 7, 2600000, 0xd81);
            setCluster(7, cores, 3100000, 0xd82);
        } else if (lower.contains("tensor g3")) {
            setCluster(0, 4, 1700000, 0xd46);
            setCluster(4, 8, 2370000, 0xd4d);
            setCluster(8, cores, 2910000, 0xd4e);
        } else if (lower.contains("tensor g2")) {
            setCluster(0, 4, 1800000, 0xd05);
            setCluster(4, 6, 2350000, 0xd41);
            setCluster(6, cores, 2850000, 0xd44);
        } else if (lower.contains("sm8750") || lower.contains("8 elite")) {
            setCluster(0, 6, 3530000, 0x001);
            setCluster(6, cores, 4320000, 0x001);
        }
        applyArray("cpu.max_frequencies", frequencies);
        applyArray("cpu.parts", parts);
        String model = value("ro.product.model", "Android device").toLowerCase(Locale.ROOT);
        long fallbackRam = (model.contains("pixel 9 pro") || model.contains("pixel 10 pro")
            || model.contains("pixel 11 pro") || model.contains("xiaomi 15 ultra")) ? 16L : 12L;
        ramBytes = Math.max(256L * 1024 * 1024,
            Math.min(256L * 1024 * 1024 * 1024, longValue("hardware.ram_bytes", fallbackRam * 1024 * 1024 * 1024)));
        directoryView = Collections.unmodifiableMap(createDirectories());
        fileView = Collections.unmodifiableMap(createFiles());
    }

    public boolean enabled(String field) {
        if (!active) return false;
        String toggle = values.get("spoof.enabled." + field);
        return toggle == null || "1".equals(toggle) || "true".equalsIgnoreCase(toggle);
    }

    public String value(String key, String fallback) {
        String value = values.get(key);
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    public String marketingName() { return value("hardware.market_name", value("ro.product.model", "Android device")); }
    public String kernelRelease() {
        return value("kernel.release", "6.6.0-android" + value("ro.build.version.release", "15")
            + "-" + value("ro.build.version.incremental", "release"));
    }
    public String kernelVersion() {
        return value("kernel.version", "#1 SMP PREEMPT " + value("ro.build.date", "release build"));
    }
    public String machine() {
        String abi = value("ro.product.cpu.abi", "arm64-v8a");
        return value("kernel.machine", abi.contains("arm64") ? "aarch64" : abi.equals("x86_64") ? "x86_64"
            : abi.equals("x86") ? "i686" : "armv7l");
    }

    public int frequency(int cpu) { return frequencies[Math.max(0, Math.min(cpu, cores - 1))]; }
    public int maxFrequency() { return Arrays.stream(frequencies).max().orElse(2800000); }
    public String minFrequency() { return String.valueOf(Math.max(1000, Math.min(integer("cpu.min_frequency", 300000), maxFrequency()))); }
    public String cacheL3() {
        String soc = value("ro.soc.model", "").toLowerCase(Locale.ROOT);
        return value("cpu.cache_l3", soc.contains("tensor g5") ? "12288K" : soc.contains("tensor g6")
            || soc.contains("sm8750") || soc.contains("elite") ? "16384K" : soc.contains("sm8450")
            || soc.contains("gen 1") ? "6144K" : soc.contains("tensor") ? "8192K" : "16384K");
    }
    public String ramType() { return value("hardware.ram_type", "LPDDR5X"); }

    public String javaProperty(String key) {
        if ("os.version".equals(key)) return enabled("kernel") ? kernelRelease() : null;
        if ("os.name".equals(key)) return enabled("kernel") ? "Linux" : null;
        if ("os.arch".equals(key)) return enabled("cpu_abi") ? machine() : null;
        return null;
    }

    public String graphicsDriver() {
        return value("hardware.graphics_driver", graphicsDriverFor(value("gpu.vendor", ""), value("gpu.renderer", ""), value("ro.soc.model", "")));
    }

    public static String graphicsDriverFor(String vendor, String renderer, String soc) {
        String gpu = ((vendor == null ? "" : vendor) + " " + (renderer == null ? "" : renderer)).toLowerCase(Locale.ROOT);
        if (gpu.contains("imagination") || gpu.contains("powervr")) return "powervr";
        if (gpu.contains("adreno") || gpu.contains("qualcomm")) return "adreno";
        if (gpu.contains("mali") || gpu.contains("arm")) return "mali";
        String chip = soc == null ? "" : soc.toLowerCase(Locale.ROOT);
        return chip.contains("tensor g5") || chip.contains("tensor g6") ? "powervr" : chip.contains("tensor") ? "mali" : "adreno";
    }

    public String macAddress(String interfaceName) {
        String supplied = value("network." + interfaceName + "_mac", "");
        if (supplied.matches("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}")) return supplied.toLowerCase(Locale.ROOT);
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest((value("ro.build.fingerprint", marketingName())
                + ":" + interfaceName).getBytes(StandardCharsets.UTF_8));
            bytes[0] = 2;
            StringBuilder text = new StringBuilder();
            for (int index = 0; index < 6; index++) {
                if (index > 0) text.append(':');
                text.append(String.format(Locale.ROOT, "%02x", bytes[index] & 255));
            }
            return text.toString();
        } catch (Exception ignored) { return "02:00:00:00:00:01"; }
    }

    /** Returns null for unknown options or a disabled kernel view; callers then preserve the real command. */
    public String unameOutput(List<String> arguments) {
        if (!enabled("kernel")) return null;
        Set<Character> selected = new LinkedHashSet<>();
        if (arguments.isEmpty()) selected.add('s');
        for (String argument : arguments) {
            if (!argument.startsWith("-") || argument.length() < 2) return null;
            for (int index = 1; index < argument.length(); index++) {
                char flag = argument.charAt(index);
                if (flag == 'a') for (char item : "snrvmo".toCharArray()) selected.add(item);
                else if ("snrvmpio".indexOf(flag) >= 0) selected.add(flag);
                else return null;
            }
        }
        List<String> output = new ArrayList<>();
        for (char flag : "snrvmpio".toCharArray()) {
            if (!selected.contains(flag)) continue;
            output.add(flag == 's' ? "Linux" : flag == 'n' ? value("kernel.hostname", "localhost")
                : flag == 'r' ? kernelRelease() : flag == 'v' ? kernelVersion() : flag == 'o' ? "Android" : machine());
        }
        return String.join(" ", output) + "\n";
    }

    /** OEM/partition aliases always resolve to the selected identity, not host ROM data. */
    public String propertyOverride(String key) {
        if (!active || key == null || HookSafetyPolicy.isRuntimeStateProperty(key)) return null;
        String lower = key.toLowerCase(Locale.ROOT);
        if (lower.contains("marketname") || lower.contains("market.name") || lower.contains("market_name")
            || lower.contains("marketing_name") || lower.equals("ro.product.nickname")
            || lower.equals("ro.config.devicename") || lower.equals("persist.sys.devicename")
            || lower.equals("ro.semc.product.name") || lower.equals("ro.display.series")
            || lower.equals("ro.product.display") || lower.equals("ro.vendor.product.display")
            || lower.equals("ro.vendor.product.ztename") || lower.equals("ro.odm.lenovo.series")
            || lower.equals("ro.vivo.camera.watermark") || lower.equals("ro.product.publicname")
            || lower.equals("ro.lge.petname") || lower.equals("ro.boot.vendor.lge.petname")
            || lower.equals("ro.vendor.asus.product.mkt_name") || lower.equals("ro.sh.brand")) {
            return enabled("model") ? marketingName() : null;
        }
        if ((lower.startsWith("ro.product.") || lower.startsWith("ro.vendor.product."))
            && (lower.endsWith(".brand") || lower.endsWith(".manufacturer") || lower.endsWith(".model")
                || lower.endsWith(".name") || lower.endsWith(".device") || lower.endsWith(".board"))) {
            String suffix = lower.substring(lower.lastIndexOf('.') + 1);
            String field = suffix.equals("name") ? "product" : suffix;
            return enabled(field) ? values.get("ro.product." + suffix) : null;
        }
        if (lower.equals("ro.product.model.bbk") || lower.endsWith(".modelname")) {
            return enabled("model") ? marketingName() : null;
        }
        if (lower.contains(".build.") && (lower.endsWith(".description") || lower.endsWith(".flavor")
            || lower.endsWith(".fingerprint") || lower.endsWith(".id") || lower.endsWith(".display.id")
            || lower.endsWith(".version.release") || lower.endsWith(".version.sdk")
            || lower.endsWith(".version.security_patch") || lower.endsWith(".version.incremental"))) {
            String suffix = lower.substring(lower.indexOf(".build.") + 7);
            String source = "ro.build." + suffix;
            String field = suffix.equals("description") ? "build_description" : suffix.equals("flavor") ? "build_flavor"
                : suffix.equals("fingerprint") ? "fingerprint" : suffix.equals("version.sdk") ? "sdk"
                : suffix.equals("version.release") ? "android_release" : suffix.equals("version.security_patch") ? "security_patch"
                : suffix.equals("version.incremental") ? "build_incremental" : suffix.equals("display.id") ? "build_display_id" : "build_id";
            return enabled(field) ? values.get(source) : null;
        }
        if (lower.equals("ro.vendor.soc.model") || lower.equals("ro.vendor.soc.model.external_name")
            || lower.equals("ro.vendor.soc.model.part_name") || lower.equals("ro.soc.model")
            || lower.equals("ro.chipname") || lower.equals("ro.hardware.chipname")
            || lower.equals("ro.unicpu.model") || lower.equals("ro.hardware.alter") || lower.equals("ro.board.chiptype")) {
            return enabled("soc_model") ? value("ro.soc.model", value("cpu.hardware", "ARM")) : null;
        }
        if (lower.equals("ro.vendor.soc.manufacturer") || lower.equals("ro.soc.manufacturer")) {
            return enabled("soc_manufacturer") ? value("ro.soc.manufacturer", value("ro.product.manufacturer", "Android")) : null;
        }
        if (lower.equals("vendor.gpu.model") || lower.equals("ro.vendor.gpu.model") || lower.equals("ro.hardware.gpu")) {
            return enabled("gpu_renderer") ? values.get("gpu.renderer") : null;
        }
        if (lower.equals("ro.vendor.gpu.vendor")) return enabled("gpu_vendor") ? values.get("gpu.vendor") : null;
        if (lower.equals("ro.hardware.egl") || lower.equals("ro.hardware.vulkan") || lower.equals("ro.boot.hardware.vulkan")) {
            return enabled("gpu_renderer") ? graphicsDriver() : null;
        }
        if (lower.equals("ro.boot.ddr_size") || lower.equals("ro.vendor.ddr_size")) {
            return enabled("ram") ? String.valueOf(ramBytes) : null;
        }
        if (lower.equals("ro.boot.hardware.ddr")) return enabled("ram") ? "RAM," + ramType() : null;
        if (lower.equals("ro.vendor.ram.type") || lower.equals("ro.vendor.ddr.type")) return enabled("ram") ? ramType() : null;
        String soc = value("ro.soc.manufacturer", "").toLowerCase(Locale.ROOT);
        if (enabled("soc_model") && !soc.contains("mediatek") && !soc.contains("mtk")
            && (lower.startsWith("ro.mediatek.") || lower.startsWith("ro.vendor.mediatek.")
                || lower.startsWith("ro.vendor.mtk_") || lower.startsWith("ro.mtk."))) return "";
        String explicit = values.get(key);
        if (explicit != null) return explicit;
        String brand = value("ro.product.brand", "android").toLowerCase(Locale.ROOT);
        if (!(brand.contains("vivo") || brand.contains("iqoo")) && (lower.startsWith("ro.vivo.")
            || lower.startsWith("ro.vendor.vivo.") || lower.startsWith("persist.vivo.")
            || lower.startsWith("vendor.gsm.project.") || lower.endsWith(".bbk"))) return "";
        return null;
    }

    public Map<String, String> propertyAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        String[] names = {"ro.product.nickname", "ro.config.marketing_name", "ro.config.devicename", "persist.sys.devicename",
            "ro.product.odm.marketname", "ro.vendor.vivo.market.name", "ro.vivo.market.name", "ro.oppo.market.name",
            "ro.vendor.oplus.market.name", "ro.vendor.config.lgsi.market_name", "ro.product.model.bbk", "ro.semc.product.name",
            "ro.vendor.soc.model", "ro.vendor.soc.model.external_name", "ro.vendor.soc.model.part_name", "ro.vendor.soc.manufacturer",
            "ro.chipname", "ro.hardware.chipname", "vendor.gpu.model", "ro.vendor.gpu.model", "ro.vendor.gpu.vendor",
            "ro.display.series", "ro.vendor.product.display", "ro.product.display", "ro.vendor.product.ztename",
            "ro.odm.lenovo.series", "ro.vivo.camera.watermark", "ro.product.publicname", "ro.lge.petname",
            "ro.boot.vendor.lge.petname", "ro.vendor.asus.product.mkt_name", "ro.sh.brand", "ro.unicpu.model",
            "ro.hardware.alter", "ro.board.chiptype", "ro.vendor.mediatek.platform", "ro.mediatek.platform",
            "ro.mediatek.version.branch", "ro.mediatek.version.release", "ro.vendor.mediatek.version.branch",
            "ro.vendor.mediatek.version.release", "ro.vendor.mtk_config_max_dram_size", "ro.boot.ddr_size",
            "ro.vendor.ddr_size", "ro.boot.hardware.ddr", "ro.vendor.ram.type", "ro.vendor.ddr.type",
            "ro.hardware.egl", "ro.hardware.vulkan", "ro.boot.hardware.vulkan"};
        for (String key : names) addAlias(aliases, key);
        for (String partition : new String[]{"system", "system_ext", "vendor", "vendor_dlkm", "odm", "product", "bootimage", "system_dlkm"}) {
            for (String field : new String[]{"brand", "manufacturer", "model", "name", "device", "board"}) {
                addAlias(aliases, "ro.product." + partition + "." + field);
            }
            for (String field : new String[]{"description", "flavor", "fingerprint", "id", "display.id", "version.release", "version.sdk", "version.security_patch", "version.incremental"}) {
                addAlias(aliases, "ro." + partition + ".build." + field);
            }
        }
        return aliases;
    }

    public String sensorName(int type) { return component(sensorKey(type), sensorLabel(type)); }
    public String sensorVendor(int type) {
        return value("sensor." + sensorKey(type) + ".vendor", value("ro.product.manufacturer", "Android"));
    }
    public String component(String key, String label) {
        return value("hardware." + key, value("sensor." + key + ".name", value("ro.product.manufacturer", "Android") + " " + label));
    }
    public static String sensorKey(int type) {
        switch (type) {
            case 1: return "accelerometer"; case 2: return "magnetometer"; case 4: return "gyroscope";
            case 5: return "light"; case 6: return "barometer"; case 8: return "proximity";
            case 9: return "gravity"; case 10: return "linear_acceleration"; case 11: return "rotation_vector";
            case 18: return "step_detector"; case 19: return "step_counter";
            default: return "type_" + type;
        }
    }
    private static String sensorLabel(int type) { return sensorKey(type).replace('_', ' '); }

    /** Only identity inventory is virtualized. Runtime/control files are left alone. */
    public Map<String, String> files() { return fileView; }
    public Map<String, List<String>> directories() { return directoryView; }

    private Map<String, String> createFiles() {
        Map<String, String> files = new LinkedHashMap<>();
        if (!active) return files;
        if (enabled("kernel")) {
            files.put("/proc/version", "Linux version " + kernelRelease() + " ("
                + value("ro.build.user", "android-build") + "@" + value("ro.build.host", "build-host")
                + ") (Android clang) " + kernelVersion() + "\n");
            files.put("/proc/sys/kernel/osrelease", kernelRelease() + "\n");
            files.put("/proc/sys/kernel/version", kernelVersion() + "\n");
            files.put("/proc/sys/kernel/ostype", "Linux\n");
            files.put("/proc/sys/kernel/hostname", value("kernel.hostname", "localhost") + "\n");
        }
        if (enabled("ram")) {
            long total = ramBytes / 1024;
            files.put("/proc/meminfo", "MemTotal:       " + total + " kB\nMemFree:        " + total / 3
                + " kB\nMemAvailable:   " + total / 2 + " kB\nBuffers:        131072 kB\nCached:         "
                + total / 8 + " kB\nSwapCached:          0 kB\nActive:         " + total / 4
                + " kB\nInactive:       " + total / 8 + " kB\nSwapTotal:            0 kB\nSwapFree:             0 kB\n");
        }
        if (enabled("cpu_cores") && enabled("cpu_hardware") && enabled("soc_model")) {
            files.put("/proc/cpuinfo", cpuInfo());
            for (String name : new String[]{"present", "online", "possible"}) files.put("/sys/devices/system/cpu/" + name, cpuRange(0, cores) + "\n");
            files.put("/sys/devices/system/cpu/offline", "\n");
            files.put("/sys/devices/system/cpu/soc", value("ro.soc.model", value("cpu.hardware", "ARM")) + "\n");
            for (String base : new String[]{"/sys/devices/soc0/", "/sys/bus/soc/devices/soc0/"}) {
                files.put(base + "soc_id", value("ro.soc.model", "ARM") + "\n");
                files.put(base + "machine", value("ro.soc.model", "ARM") + "\n");
                files.put(base + "family", value("ro.soc.manufacturer", "ARM") + "\n");
                files.put(base + "revision", value("cpu.revision", "0") + "\n");
            }
            for (int cpu = 0; cpu < cores; cpu++) {
                String group = group(cpu);
                String prefix = "/sys/devices/system/cpu/cpu" + cpu + "/";
                files.put(prefix + "online", "1\n");
                files.put(prefix + "regs/identification/midr_el1", String.format(Locale.ROOT, "0x%08x\n", midr(cpu)));
                files.put(prefix + "topology/core_id", cpu + "\n");
                files.put(prefix + "topology/physical_package_id", "0\n");
                files.put(prefix + "topology/cluster_id", cluster(cpu) + "\n");
                files.put(prefix + "cpu_capacity", Math.max(1, (int) ((long) frequencies[cpu] * 1024 / maxFrequency())) + "\n");
                files.put(prefix + "topology/core_siblings_list", cpuRange(0, cores) + "\n");
                files.put(prefix + "topology/thread_siblings_list", cpu + "\n");
                for (String frequency : new String[]{"cpuinfo_max_freq", "scaling_max_freq", "scaling_cur_freq", "cpuinfo_cur_freq"}) {
                    files.put(prefix + "cpufreq/" + frequency, frequencies[cpu] + "\n");
                }
                for (String frequency : new String[]{"cpuinfo_min_freq", "scaling_min_freq"}) files.put(prefix + "cpufreq/" + frequency, minFrequency() + "\n");
                files.put(prefix + "cpufreq/related_cpus", group + "\n");
                files.put(prefix + "cpufreq/affected_cpus", group + "\n");
                files.put(prefix + "cpufreq/scaling_available_frequencies", minFrequency() + " " + frequencies[cpu] + "\n");
                files.put(prefix + "cache/index3/size", cacheL3() + "\n");
                files.put(prefix + "cache/index3/level", "3\n");
                files.put(prefix + "cache/index3/type", "Unified\n");
                files.put(prefix + "cache/index3/shared_cpu_list", cpuRange(0, cores) + "\n");
            }
            Set<Integer> covered = new LinkedHashSet<>();
            for (int cpu = 0; cpu < cores; cpu++) {
                if (!covered.add(frequencies[cpu])) continue;
                String prefix = "/sys/devices/system/cpu/cpufreq/policy" + cpu + "/";
                for (String field : new String[]{"cpuinfo_max_freq", "scaling_max_freq", "scaling_cur_freq", "cpuinfo_cur_freq"}) {
                    files.put(prefix + field, frequencies[cpu] + "\n");
                }
                for (String field : new String[]{"cpuinfo_min_freq", "scaling_min_freq"}) files.put(prefix + field, minFrequency() + "\n");
                files.put(prefix + "related_cpus", group(cpu) + "\n");
                files.put(prefix + "affected_cpus", group(cpu) + "\n");
            }
        }
        if (enabled("sensor_metadata")) {
            files.put("/sys/class/sensors/accelerometer_sensor/name", sensorName(1) + "\n");
            files.put("/sys/class/sensors/magnetic_sensor/name", sensorName(2) + "\n");
            files.put("/sys/class/sensors/gyro_sensor/name", sensorName(4) + "\n");
        }
        if (enabled("hardware_components")) {
            files.put("/proc/asound/cards", " 0 [" + token(component("audio", "audio")) + "]: " + component("audio", "audio") + "\n");
            files.put("/proc/asound/card0/id", token(component("audio", "audio")) + "\n");
            files.put("/proc/asound/pcm", "00-00: " + component("audio", "audio") + " : playback 1 : capture 1\n");
            files.put("/proc/modules", "");
            files.put("/proc/interrupts", "");
            files.put("/proc/bus/input/devices", "");
            for (Map.Entry<String, List<String>> entry : directoryView.entrySet()) {
                if (!entry.getKey().startsWith("/sys/bus/") || !entry.getKey().endsWith("/drivers")) continue;
                for (String driver : entry.getValue()) {
                    String prefix = entry.getKey() + "/" + driver + "/";
                    files.put(prefix + "uevent", "DRIVER=" + driver + "\n");
                    String node = entry.getKey().contains("/i2c/") ? "1-0001" : "profile_device";
                    files.put(prefix + node + "/name", driver + "\n");
                    files.put(prefix + node + "/modalias", "platform:" + driver + "\n");
                }
            }
            files.put("/sys/class/regulator/regulator.0/name", component("pmic", "PMIC") + "\n");
            for (String base : new String[]{"/proc/device-tree/", "/sys/firmware/devicetree/base/"}) {
                files.put(base + "model", marketingName() + "\0");
                files.put(base + "compatible", token(value("ro.product.manufacturer", "android")) + ","
                    + token(value("ro.product.device", "device")) + "\0");
                files.put(base + "soc/compatible", token(value("ro.soc.model", "arm")) + "\0");
            }
        }
        if (enabled("ram")) {
            files.put("/proc/ddr_rod", ramType() + "\n");
            files.put("/sys/bus/platform/drivers/ddr_type/ddr_type", ramType() + "\n");
            files.put("/sys/class/mi_memory/mi_memory_device/ddr/ddr_vendor", value("hardware.ram_vendor", "Configured") + "\n");
            files.put("/proc/vendor_imem/ddr_id", value("hardware.ram_vendor", "Configured") + "-" + ramType() + "\n");
        }
        if (enabled("network_metadata")) {
            files.put("/sys/class/net/wlan0/address", macAddress("wifi") + "\n");
            files.put("/sys/class/net/wifi0/address", macAddress("wifi") + "\n");
        }
        return Collections.unmodifiableMap(files);
    }

    private Map<String, List<String>> createDirectories() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (!active) return result;
        if (enabled("hardware_components")) {
            List<String> drivers = new ArrayList<>();
            for (String component : new String[]{"accelerometer", "light", "proximity", "magnetometer", "gyroscope", "charger", "pmic", "nfc", "audio", "fingerprint", "wifi", "sound", "other"}) {
                drivers.add(token(component(component, component)));
            }
            for (String bus : new String[]{"platform", "i2c", "spi", "iio", "sdio", "mdio_bus", "pci"}) {
                String base = "/sys/bus/" + bus + "/drivers";
                result.put(base, Collections.unmodifiableList(drivers));
                for (String driver : drivers) result.put(base + "/" + driver,
                    Collections.singletonList(bus.equals("i2c") ? "1-0001" : "profile_device"));
            }
            result.put("/sys/class/sound", Collections.unmodifiableList(Arrays.asList("card0", "pcmC0D0p", "pcmC0D0c")));
            result.put("/sys/class/regulator", Collections.singletonList("regulator.0"));
            result.put("/sys/devices/virtual/devlink", Collections.emptyList());
            if (!value("ro.soc.manufacturer", "").toLowerCase(Locale.ROOT).contains("mediatek")) {
                result.put("/sys/devices/platform/mt-pmic", Collections.emptyList());
            }
            for (String base : new String[]{"/proc/device-tree", "/sys/firmware/devicetree/base"}) {
                result.put(base, Collections.unmodifiableList(Arrays.asList("model", "compatible", "soc", "memory", "cpus", "chosen")));
            }
        }
        return result;
    }

    public String cpuInfo() {
        StringBuilder text = new StringBuilder();
        for (int cpu = 0; cpu < cores; cpu++) {
            text.append("processor\t: ").append(cpu).append("\nBogoMIPS\t: 38.40\nFeatures\t: ")
                .append(value("cpu.features", "fp asimd evtstrm aes pmull sha1 sha2 crc32 atomics fphp asimdhp cpuid asimdrdm jscvt fcma lrcpc dcpop asimddp sha512"))
                .append("\nCPU implementer\t: ").append(value("cpu.implementer", value("ro.soc.manufacturer", "ARM").toLowerCase(Locale.ROOT).contains("qualcomm") ? "0x51" : "0x41"))
                .append("\nCPU architecture: 8\nCPU variant\t: ").append(value("cpu.variant", "0x0"))
                .append("\nCPU part\t: ").append(String.format(Locale.ROOT, "0x%03x", parts[cpu]))
                .append("\nCPU revision\t: ").append(value("cpu.revision", "0")).append("\n\n");
        }
        return text.append("Hardware\t: ").append(value("cpu.hardware", value("ro.soc.model", "ARM"))).append("\n").toString();
    }

    private int midr(int cpu) {
        int implementer;
        String fallback = value("ro.soc.manufacturer", "ARM").toLowerCase(Locale.ROOT).contains("qualcomm") ? "0x51" : "0x41";
        try { implementer = Integer.decode(value("cpu.implementer", fallback)); } catch (NumberFormatException ignored) { implementer = Integer.decode(fallback); }
        int variant = 0, revision = 0;
        try { variant = Integer.decode(value("cpu.variant", "0")); revision = Integer.decode(value("cpu.revision", "0")); }
        catch (NumberFormatException ignored) { }
        return ((implementer & 255) << 24) | ((variant & 15) << 20) | (15 << 16) | ((parts[cpu] & 4095) << 4) | (revision & 15);
    }
    private String group(int cpu) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < cores; i++) if (frequencies[i] == frequencies[cpu]) { if (text.length() > 0) text.append(' '); text.append(i); }
        return text.toString();
    }
    private int cluster(int cpu) {
        Set<Integer> unique = new LinkedHashSet<>();
        for (int i = 0; i <= cpu; i++) unique.add(frequencies[i]);
        return unique.size() - 1;
    }
    private void setCluster(int first, int end, int frequency, int part) {
        for (int i = first; i < Math.min(end, cores); i++) { frequencies[i] = frequency; parts[i] = part; }
    }
    private void applyArray(String key, int[] target) {
        String value = values.get(key);
        if (value == null) return;
        String[] pieces = value.trim().split("[,\\s]+");
        if (pieces.length != target.length) return;
        int[] parsed = target.clone();
        try {
            for (int i = 0; i < parsed.length; i++) {
                parsed[i] = Integer.decode(pieces[i]);
                if (key.equals("cpu.max_frequencies") ? parsed[i] < 1000 || parsed[i] > 10000000
                    : parsed[i] < 0 || parsed[i] > 0xfff) return;
            }
        }
        catch (NumberFormatException ignored) { return; }
        System.arraycopy(parsed, 0, target, 0, parsed.length);
    }
    private int integer(String key, int fallback) { try { return Integer.parseInt(value(key, String.valueOf(fallback))); } catch (NumberFormatException ignored) { return fallback; } }
    private long longValue(String key, long fallback) { try { return Long.parseLong(value(key, String.valueOf(fallback))); } catch (NumberFormatException ignored) { return fallback; } }
    private void addAlias(Map<String, String> map, String key) { String value = propertyOverride(key); if (value != null) map.put(key, value); }
    private static String cpuRange(int first, int end) { return end - first == 1 ? String.valueOf(first) : first + "-" + (end - 1); }
    private static String token(String value) { return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]+", "_"); }
}
