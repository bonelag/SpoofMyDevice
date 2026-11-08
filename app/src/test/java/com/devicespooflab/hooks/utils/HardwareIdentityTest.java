package com.devicespooflab.hooks.utils;

import static org.junit.Assert.*;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public class HardwareIdentityTest {
    private Map<String, String> pixel() {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("ro.product.brand", "google"); p.put("ro.product.manufacturer", "Google");
        p.put("ro.product.model", "Pixel 10 Pro"); p.put("ro.product.device", "blazer");
        p.put("ro.soc.model", "Google Tensor G5"); p.put("ro.soc.manufacturer", "Google");
        p.put("cpu.hardware", "Google Tensor G5"); p.put("cpu.cores", "8");
        p.put("ro.build.description", "blazer-user 16 BP2A.250820.005 release-keys");
        p.put("ro.build.version.sdk", "36"); p.put("ro.build.version.release", "16");
        return p;
    }

    @Test public void oemAliasesAndPartitionsUseOneIdentity() {
        HardwareIdentity i = new HardwareIdentity(pixel(), true);
        assertEquals("Pixel 10 Pro", i.propertyOverride("ro.vendor.vivo.market.name"));
        assertEquals("Pixel 10 Pro", i.propertyOverride("ro.product.nickname"));
        assertEquals("Google Tensor G5", i.propertyOverride("ro.vendor.soc.model.external_name"));
        assertEquals(pixel().get("ro.build.description"), i.propertyOverride("ro.system.build.description"));
        assertEquals("36", i.propertyOverride("ro.vendor.build.version.sdk"));
        assertNull(i.propertyOverride("sys.boot_completed"));
        assertNull(i.propertyOverride("init.svc.bootanim"));
    }

    @Test public void topologyCpuInfoAndFrequencyAgree() {
        HardwareIdentity i = new HardwareIdentity(pixel(), true);
        Map<String, String> files = i.files();
        assertEquals("0-7\n", files.get("/sys/devices/system/cpu/present"));
        assertEquals("2250000\n", files.get("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq"));
        assertEquals("3050000\n", files.get("/sys/devices/system/cpu/cpu2/cpufreq/cpuinfo_max_freq"));
        assertEquals("3780000\n", files.get("/sys/devices/system/cpu/cpu7/cpufreq/cpuinfo_max_freq"));
        assertTrue(files.get("/proc/cpuinfo").contains("CPU part\t: 0xd82"));
        assertEquals(8, files.get("/proc/cpuinfo").split("processor\\t:").length - 1);
        assertEquals(16L * 1024 * 1024 * 1024, i.ramBytes);
        assertFalse(files.containsKey("/sys/power/state"));
    }

    @Test public void disabledFieldsAndUnassignmentPreserveOriginalReads() {
        Map<String, String> p = pixel(); p.put("spoof.enabled.model", "false");
        p.put("spoof.enabled.cpu_cores", "false"); p.put("spoof.enabled.kernel", "false");
        HardwareIdentity i = new HardwareIdentity(p, true);
        assertNull(i.propertyOverride("ro.product.nickname"));
        assertFalse(i.files().containsKey("/proc/cpuinfo"));
        assertFalse(i.files().containsKey("/proc/version"));
        HardwareIdentity none = new HardwareIdentity(pixel(), false);
        assertTrue(none.files().isEmpty()); assertTrue(none.directories().isEmpty());
        assertNull(none.propertyOverride("ro.vendor.vivo.market.name"));
    }

    @Test public void customMetadataAndBoundedCountsAreRespected() {
        Map<String, String> p = pixel(); p.put("sensor.accelerometer.name", "Custom accelerometer");
        p.put("sensor.accelerometer.vendor", "Test vendor"); p.put("cpu.cores", "1000000");
        p.put("kernel.release", "6.6.123-factory");
        HardwareIdentity i = new HardwareIdentity(p, true);
        assertEquals(32, i.cores); assertEquals("Custom accelerometer", i.sensorName(1));
        assertEquals("Test vendor", i.sensorVendor(1)); assertEquals("6.6.123-factory", i.kernelRelease());
    }

    @Test public void oldOemAliasesCannotOverrideSelectedModelOrSoc() {
        Map<String, String> p = pixel();
        p.put("ro.product.nickname", "iQOO Z10 Turbo+");
        p.put("ro.vendor.soc.model", "MT6991");
        p.put("ro.vendor.mediatek.version.branch", "physical_alps_branch");
        HardwareIdentity i = new HardwareIdentity(p, true);
        assertEquals("Pixel 10 Pro", i.propertyOverride("ro.product.nickname"));
        assertEquals("Pixel 10 Pro", i.propertyOverride("ro.lge.petname"));
        assertEquals("Google Tensor G5", i.propertyOverride("ro.vendor.soc.model"));
        assertEquals("", i.propertyOverride("ro.vendor.mediatek.version.branch"));
        assertEquals("", i.propertyOverride("ro.vendor.mediatek.platform"));
    }

    @Test public void ramKernelNetworkAndPolicyViewsShareProfileData() {
        HardwareIdentity i = new HardwareIdentity(pixel(), true);
        assertEquals("17179869184", i.propertyOverride("ro.boot.ddr_size"));
        assertEquals("RAM,LPDDR5X", i.propertyOverride("ro.boot.hardware.ddr"));
        assertTrue(i.files().get("/proc/meminfo").contains("16777216 kB"));
        assertEquals("LPDDR5X\n", i.files().get("/proc/ddr_rod"));
        assertEquals(i.macAddress("wifi") + "\n", i.files().get("/sys/class/net/wlan0/address"));
        assertEquals("3050000\n", i.files().get("/sys/devices/system/cpu/cpufreq/policy2/cpuinfo_max_freq"));
        assertEquals("2 3 4 5 6\n", i.files().get("/sys/devices/system/cpu/cpufreq/policy2/related_cpus"));
        assertEquals(3780000, i.maxFrequency());
        assertEquals(i.cacheL3() + "\n", i.files().get("/sys/devices/system/cpu/cpu0/cache/index3/size"));
        assertEquals(i.kernelRelease() + "\n", i.unameOutput(java.util.Collections.singletonList("-r")));
        assertEquals(i.kernelRelease() + " " + i.machine() + "\n",
            i.unameOutput(java.util.Collections.singletonList("-rm")));
        assertNull(i.unameOutput(java.util.Collections.singletonList("--unknown")));
    }

    @Test public void inventoryHasChildrenAndNoPhysicalFallbackRoots() {
        HardwareIdentity i = new HardwareIdentity(pixel(), true);
        java.util.List<String> drivers = i.directories().get("/sys/bus/i2c/drivers");
        assertTrue(drivers.contains("google_accelerometer"));
        assertEquals(java.util.Collections.singletonList("1-0001"),
            i.directories().get("/sys/bus/i2c/drivers/google_accelerometer"));
        assertEquals("google_accelerometer\n",
            i.files().get("/sys/bus/i2c/drivers/google_accelerometer/1-0001/name"));
        assertTrue(i.directories().containsKey("/sys/bus/iio/drivers"));
        assertTrue(i.directories().get("/sys/devices/platform/mt-pmic").isEmpty());
        assertTrue(i.directories().get("/proc/device-tree").contains("model"));
        assertEquals("Pixel 10 Pro\0", i.files().get("/proc/device-tree/model"));
        assertEquals("", i.files().get("/proc/interrupts"));
    }

    @Test public void disabledMetadataKeepsItsReadsUnchanged() {
        Map<String, String> p = pixel();
        p.put("spoof.enabled.ram", "false");
        p.put("spoof.enabled.hardware_components", "false");
        p.put("spoof.enabled.network_metadata", "false");
        HardwareIdentity i = new HardwareIdentity(p, true);
        assertNull(i.propertyOverride("ro.boot.ddr_size"));
        assertFalse(i.files().containsKey("/proc/meminfo"));
        assertFalse(i.files().containsKey("/sys/class/net/wlan0/address"));
        assertFalse(i.directories().containsKey("/sys/bus/i2c/drivers"));
    }

    @Test public void invalidArraysStayBoundedAndMapsAreImmutable() {
        Map<String, String> p = pixel();
        p.put("cpu.max_frequencies", "0,0,0,0,0,0,0,0");
        HardwareIdentity i = new HardwareIdentity(p, true);
        assertEquals(3780000, i.maxFrequency());
        assertSame(i.files(), i.files());
        assertSame(i.directories(), i.directories());
        try { i.files().put("/proc/version", "physical"); fail(); }
        catch (UnsupportedOperationException expected) { }
        try { i.directories().get("/sys/class/sound").set(0, "physical"); fail(); }
        catch (UnsupportedOperationException expected) { }
    }

    @Test public void invalidMacUsesOneValidFallbackInsteadOfLeakingARealAddress() {
        Map<String, String> p = pixel();
        p.put("network.wifi_mac", "invalid address");
        HardwareIdentity i = new HardwareIdentity(p, true);
        assertTrue(i.macAddress("wifi").matches("[0-9a-f]{2}(:[0-9a-f]{2}){5}"));
        assertEquals(i.macAddress("wifi") + "\n", i.files().get("/sys/class/net/wlan0/address"));
        p.put("network.wifi_mac", "02:AA:BB:CC:DD:EE");
        assertEquals("02:aa:bb:cc:dd:ee", new HardwareIdentity(p, true).macAddress("wifi"));
    }

    @Test public void javaKernelPropertiesUseTheSameViewAsProcAndUname() {
        Map<String, String> p = pixel();
        p.put("kernel.release", "6.6-configured");
        HardwareIdentity i = new HardwareIdentity(p, true);
        assertEquals("6.6-configured", i.javaProperty("os.version"));
        assertEquals("Linux", i.javaProperty("os.name"));
        assertEquals(i.machine(), i.javaProperty("os.arch"));
        p.put("spoof.enabled.kernel", "false");
        assertNull(new HardwareIdentity(p, true).javaProperty("os.version"));
        assertNull(new HardwareIdentity(p, false).javaProperty("os.arch"));
    }

    @Test public void graphicsDriverAliasesFollowConfiguredGpuInsteadOfOldVendorDefaults() {
        Map<String, String> p = pixel();
        p.put("gpu.vendor", "Imagination Technologies");
        p.put("gpu.renderer", "PowerVR DXT-48-1536");
        p.put("ro.hardware.egl", "mali");
        HardwareIdentity i = new HardwareIdentity(p, true);
        assertEquals("powervr", i.propertyOverride("ro.hardware.egl"));
        assertEquals("powervr", i.propertyOverride("ro.boot.hardware.vulkan"));
        assertEquals("mali", HardwareIdentity.graphicsDriverFor("ARM", "Mali-G715", "Tensor G3"));
        assertEquals("adreno", HardwareIdentity.graphicsDriverFor("Qualcomm", "Adreno 830", "SM8750"));
    }
}
