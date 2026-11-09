package com.devicespooflab.hooks.utils;

import static org.junit.Assert.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class IdentityFileCacheTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private Map<String, String> files() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("/proc/version", "Configured kernel\n");
        files.put("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq", "2250000\n");
        return files;
    }

    @Test public void completedGenerationReusesTheScriptAndFilesAcrossPreparations() throws Exception {
        File directory = new File(temporary.getRoot(), "generation");
        IdentityFileCache.Prepared first = IdentityFileCache.prepare(directory, files(), Collections.emptyMap(), Collections.emptyMap());
        File script = new File(first.script);
        File kernel = new File(directory, "proc/version");
        long timestamp = 1234567890000L;
        assertTrue(script.setLastModified(timestamp));
        assertTrue(kernel.setLastModified(timestamp));
        IdentityFileCache.Prepared second = IdentityFileCache.prepare(directory, files(), Collections.emptyMap(), Collections.emptyMap());
        assertEquals(first.paths, second.paths);
        assertEquals(first.script, second.script);
        assertEquals(timestamp, script.lastModified());
        assertEquals(timestamp, kernel.lastModified());
        assertTrue(new File(directory, ".complete").isFile());
        assertEquals(new File(directory, "sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq").getAbsolutePath(),
            IdentityPaths.redirect(second.paths, "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq"));
    }

    @Test public void interruptedGenerationRebuildsPartialFilesBeforeCompletion() throws Exception {
        File directory = temporary.newFolder("partial");
        File kernel = new File(directory, "proc/version");
        assertTrue(kernel.getParentFile().mkdirs());
        Files.write(kernel.toPath(), "Partial".getBytes(StandardCharsets.UTF_8));
        IdentityFileCache.prepare(directory, files(), Collections.emptyMap(), Collections.emptyMap());
        assertEquals("Configured kernel\n", new String(Files.readAllBytes(kernel.toPath()), StandardCharsets.UTF_8));
        assertTrue(new File(directory, ".complete").isFile());
    }

    @Test public void missingScriptRebuildsAFormerlyCompleteGeneration() throws Exception {
        File directory = temporary.newFolder("missing-script");
        IdentityFileCache.prepare(directory, files(), Collections.emptyMap(), Collections.emptyMap());
        assertTrue(new File(directory, "identity.sh").delete());
        IdentityFileCache.prepare(directory, files(), Collections.emptyMap(), Collections.emptyMap());
        assertTrue(new File(directory, "identity.sh").isFile());
        assertTrue(new File(directory, ".complete").isFile());
    }

    @Test public void failedGenerationCannotPublishCompletion() throws Exception {
        File directory = temporary.newFolder("blocked");
        assertTrue(new File(directory, "proc").createNewFile());
        assertThrows(java.io.IOException.class,
            () -> IdentityFileCache.prepare(directory, files(), Collections.emptyMap(), Collections.emptyMap()));
        assertFalse(new File(directory, ".complete").exists());
    }

    @Test public void partialCacheEvictionRestoresFilesAndInventoryNodesDespiteTheMarker() throws Exception {
        File directory = temporary.newFolder("evicted");
        Map<String, java.util.List<String>> inventories = Collections.singletonMap("/sys/bus/i2c/drivers", Collections.singletonList("configured_sensor"));
        IdentityFileCache.prepare(directory, files(), inventories, Collections.emptyMap());
        File kernel = new File(directory, "proc/version");
        File sensor = new File(directory, "sys/bus/i2c/drivers/configured_sensor");
        assertTrue(kernel.delete());
        assertTrue(sensor.delete());
        assertTrue(new File(directory, ".complete").isFile());
        IdentityFileCache.prepare(directory, files(), inventories, Collections.emptyMap());
        assertEquals("Configured kernel\n", new String(Files.readAllBytes(kernel.toPath()), StandardCharsets.UTF_8));
        assertTrue(sensor.isDirectory());
        assertEquals(0, directory.list((parent, name) -> name.startsWith(".smd-")).length);
    }
}
