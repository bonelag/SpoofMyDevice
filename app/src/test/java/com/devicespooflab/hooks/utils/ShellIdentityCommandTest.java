package com.devicespooflab.hooks.utils;

import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class ShellIdentityCommandTest {
    private Map<String, String> paths() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("/proc/version", "/cache/identity/version");
        result.put("/sys/bus/i2c/drivers", "/cache/identity/drivers");
        result.put("/proc/device-tree", "/cache/identity/tree");
        return result;
    }

    @Test public void readonlyArgumentsPreserveOptionsOrderAndCallerList() {
        List<String> command = Arrays.asList("cat", "-n", "/proc//version", "/unrelated/file");
        assertEquals(Arrays.asList("cat", "-n", "/cache/identity/version", "/unrelated/file"),
            ShellIdentityCommand.rewrite(command, paths(), ""));
        assertEquals("/proc//version", command.get(2));
        assertEquals(Arrays.asList("/system/bin/ls", "-1", "/cache/identity/drivers"),
            ShellIdentityCommand.rewrite(Arrays.asList("/system/bin/ls", "-1", "/sys/bus/i2c/drivers/"), paths(), ""));
    }

    @Test public void multicallAndDeviceTreeReadersUseTheSamePaths() {
        assertEquals(Arrays.asList("toybox", "od", "-An", "-tx", "/cache/identity/tree/memory/dram_type"),
            ShellIdentityCommand.rewrite(Arrays.asList("toybox", "od", "-An", "-tx", "/proc/device-tree/memory/dram_type"), paths(), ""));
    }

    @Test public void shellSourceKeepsCompoundInputLiteralAndIsNotAddedTwice() {
        String caller = "DIR_PATH='/sys/bus/i2c/drivers'; cd \"$DIR_PATH\"; for f in *; do echo \"$f\"; done";
        List<String> command = Arrays.asList("su", "-c", caller);
        List<String> wrapped = ShellIdentityCommand.rewrite(command, paths(), "/cache/profile ' one.sh");
        assertEquals(". '/cache/profile '\\'' one.sh'; " + caller, wrapped.get(2));
        assertEquals(caller, command.get(2));
        assertNull(ShellIdentityCommand.rewrite(wrapped, paths(), "/cache/profile ' one.sh"));
        assertNull(ShellIdentityCommand.rewrite(command, paths(), ""));
    }

    @Test public void writersBootControlsAndUnknownProgramsPassThrough() {
        assertNull(ShellIdentityCommand.rewrite(Arrays.asList("tee", "/proc/version"), paths(), ""));
        assertNull(ShellIdentityCommand.rewrite(Arrays.asList("cat", "/sys/power/state"), paths(), ""));
        assertNull(ShellIdentityCommand.rewrite(Arrays.asList("custom", "/proc/version"), paths(), ""));
        assertNull(ShellIdentityCommand.rewrite(Collections.emptyList(), paths(), ""));
    }
}
