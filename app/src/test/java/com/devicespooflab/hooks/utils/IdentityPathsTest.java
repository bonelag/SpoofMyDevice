package com.devicespooflab.hooks.utils;

import static org.junit.Assert.*;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public class IdentityPathsTest {
    @Test public void posixNormalizationDoesNotDependOnHostOs() {
        assertEquals("/sys/bus/i2c/drivers", IdentityPaths.normalize("/sys//bus/i2c/./drivers/"));
        assertEquals("/proc/version", IdentityPaths.normalize("/proc/cpuinfo/../version"));
        assertEquals("/", IdentityPaths.normalize("/../../"));
        assertNull(IdentityPaths.normalize("proc/version"));
        assertNull(IdentityPaths.normalize("/proc/\0version"));
    }

    @Test public void mostSpecificInventoryOwnsItsChildren() {
        Map<String, String> paths = new LinkedHashMap<>();
        paths.put("/sys/devices/system/cpu", "/cache/cpu");
        paths.put("/sys/devices/system/cpu/cpu0/cpufreq", "/cache/policy0");
        assertEquals("/cache/policy0/cpuinfo_max_freq",
            IdentityPaths.redirect(paths, "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq"));
        assertEquals("/cache/cpu", IdentityPaths.redirect(paths, "/sys/devices/system/cpu/"));
        assertNull(IdentityPaths.redirect(paths, "/sys/devices/system/cpufreq"));
        assertNull(IdentityPaths.redirect(paths, "/cache/cpu"));
    }

    @Test public void unknownChildrenStayInsideClosedInventories() {
        Map<String, String> paths = new LinkedHashMap<>();
        paths.put("/sys/bus/i2c/drivers", "/cache/drivers");
        paths.put("/proc/device-tree", "/cache/tree");
        assertEquals("/cache/drivers/physical_driver",
            IdentityPaths.redirect(paths, "/sys/bus/i2c/drivers/physical_driver"));
        assertEquals("/cache/tree/memory/dram_type",
            IdentityPaths.redirect(paths, "/proc/device-tree/memory/dram_type"));
        assertNull(IdentityPaths.redirect(paths, "/sys/power/state"));
    }

    @Test public void procFilesCannotActAsDirectoryPrefixes() {
        Map<String, String> paths = new LinkedHashMap<>();
        paths.put("/proc/version", "/cache/version");
        assertEquals("/cache/version", IdentityPaths.redirect(paths, "/proc//version"));
        assertNull(IdentityPaths.redirect(paths, "/proc/version/child"));
        assertNull(IdentityPaths.redirect(paths, "/proc/version/../sys/kernel/osrelease"));
    }
}
