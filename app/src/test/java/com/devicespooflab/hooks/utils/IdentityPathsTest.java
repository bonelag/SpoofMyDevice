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

    @Test public void ordinaryCanonicalPathsReuseTheOriginalString() {
        String path = new String("/data/user/0/example/cache/database.db");
        assertSame(path, IdentityPaths.normalize(path));
        assertEquals("/sys/devices/system/cpu", IdentityPaths.normalize("/data/../sys/devices/./system/cpu/"));
    }

    @Test public void lookupCostIsBoundedByDepthInsteadOfInventorySize() {
        class LookupMap extends LinkedHashMap<String, String> {
            int lookups;
            @Override public String get(Object key) { lookups++; return super.get(key); }
            @Override public java.util.Set<String> keySet() { throw new AssertionError("File reads must not scan the inventory"); }
        }
        LookupMap paths = new LookupMap();
        for (int index = 0; index < 4096; index++) paths.put("/sys/bus/platform/drivers/driver" + index, "/cache/driver" + index);
        paths.put("/sys/devices/system/cpu", "/cache/cpu");
        assertEquals("/cache/cpu/cpu0/cpufreq/scaling_cur_freq",
            IdentityPaths.redirect(paths, "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq"));
        assertTrue(paths.lookups <= 8);
        paths.lookups = 0;
        assertNull(IdentityPaths.redirect(paths, "/data/user/0/example/cache/database.db"));
        assertTrue(paths.lookups <= 8);
    }

    @Test public void compactionPreservesExceptionsAndExactProcFiles() {
        Map<String, String> paths = new LinkedHashMap<>();
        paths.put("/sys/devices/system/cpu", "/cache/cpu");
        paths.put("/sys/devices/system/cpu/cpu0", "/cache/cpu/cpu0");
        paths.put("/sys/devices/system/cpu/cpu0/cpufreq", "/cache/policy");
        paths.put("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq", "/cache/policy/scaling_cur_freq");
        paths.put("/proc/version", "/cache/version");
        paths.put("/proc/version/child", "/cache/version/child");
        Map<String, String> compact = IdentityPaths.compact(paths);
        assertEquals(4, compact.size());
        for (String path : paths.keySet()) assertEquals(IdentityPaths.redirect(paths, path), IdentityPaths.redirect(compact, path));
        assertEquals("/cache/policy/unknown", IdentityPaths.redirect(compact, "/sys/devices/system/cpu/cpu0/cpufreq/unknown"));
        assertNull(IdentityPaths.redirect(compact, "/proc/version/unknown"));
    }

    @Test public void preparedIndexIsImmutableAndHandlesRootChangingPaths() {
        Map<String, String> paths = new LinkedHashMap<>();
        paths.put("/sys/devices/system/cpu", "/cache/cpu");
        IdentityPaths.Index index = new IdentityPaths.Index(paths);
        paths.clear();
        assertEquals("/cache/cpu/cpu0", index.redirect("/data/../sys//devices/system/cpu/cpu0"));
        assertNull(index.redirect("/data/user/0/example/cache/smd_identity/file"));
        assertNull(index.redirect("/system/fonts/Roboto.ttf"));
        assertNull(index.redirect("/systematic/file"));
        assertThrows(UnsupportedOperationException.class, () -> index.paths().clear());
    }
}
