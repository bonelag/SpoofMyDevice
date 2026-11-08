package com.devicespooflab.hooks.utils;

import static org.junit.Assert.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;
import org.junit.Test;
import org.junit.Rule;
import org.junit.Assume;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class ShellIdentityScriptTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    @Test public void profileStringsStayLiteralAndBootControlsAreNotReplaced() {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("ro.product.model", "Pixel ' $(touch sentinel)");
        properties.put("sys.boot_completed", "0"); properties.put("init.svc.bootanim", "running");
        String script = ShellIdentityScript.build(properties, Collections.singletonMap("/proc/version", "/cache/identity version"));
        assertTrue(script.contains("'Pixel '\\'' $(touch sentinel)'"));
        assertTrue(script.contains("'/cache/identity version'"));
        assertFalse(script.contains("'sys.boot_completed')")); assertFalse(script.contains("'init.svc.bootanim')"));
        assertTrue(script.contains("command /system/bin/getprop \"$@\""));
    }

    @Test public void executedDumpAddsMissingAliasesAndKeepsBootStateAndLiteralValues() throws Exception {
        Map<String, String> p = properties();
        p.put("ro.product.model", "Pixel ' $(touch sentinel)");
        p.put("sys.boot_completed", "0");
        p.put("init.svc.bootanim", "running");
        String output = execute(ShellIdentityScript.build(p, Collections.emptyMap()), "getprop\ngetprop ro.product.model\n");
        assertTrue(output.contains("[ro.product.model]: [Pixel ' $(touch sentinel)]"));
        assertEquals(1, output.split("\\[ro.product.nickname\\]:", -1).length - 1);
        assertTrue(output.contains("[sys.boot_completed]: [1]"));
        assertTrue(output.contains("[init.svc.bootanim]: [stopped]"));
        assertFalse(output.contains("iQOO"));
        assertFalse(output.contains("physical_alps"));
        assertFalse(new File(temporary.getRoot(), "sentinel").exists());
    }

    @Test public void executedReadersHandleTrailingSlashesOptionsCdAndRelativeGlobs() throws Exception {
        File drivers = temporary.newFolder("drivers");
        File sensor = new File(drivers, "profile_accelerometer/1-0001");
        assertTrue(sensor.mkdirs());
        Files.write(new File(sensor, "name").toPath(), "Configured accelerometer\n".getBytes(StandardCharsets.UTF_8));
        Map<String, String> paths = Collections.singletonMap("/sys/bus/i2c/drivers", posix(drivers));
        String commands = "ls -1 '/sys//bus/i2c/./drivers/'\ncd '/sys/bus/i2c/drivers/'\npwd\n"
            + "for driver in *; do for node in \"$driver\"/*; do cat \"$node/name\"; done; done\n";
        String output = execute(ShellIdentityScript.build(properties(), paths), commands);
        assertEquals("profile_accelerometer\n/sys/bus/i2c/drivers\nConfigured accelerometer\n", output);
    }

    @Test public void executedCatPreservesMultipleArgumentsAndNumberingOptions() throws Exception {
        File kernel = temporary.newFile("kernel");
        File memory = temporary.newFile("memory");
        Files.write(kernel.toPath(), "Configured kernel\n".getBytes(StandardCharsets.UTF_8));
        Files.write(memory.toPath(), "Configured memory\n".getBytes(StandardCharsets.UTF_8));
        Map<String, String> paths = new LinkedHashMap<>();
        paths.put("/proc/version", posix(kernel));
        paths.put("/proc/meminfo", posix(memory));
        String output = execute(ShellIdentityScript.build(properties(), paths),
            "cat /proc//version /proc/./meminfo\ncat -n /proc/version\n");
        assertTrue(output.startsWith("Configured kernel\nConfigured memory\n"));
        assertTrue(output.matches("(?s).*\\s1\\s+Configured kernel\\n"));
    }

    @Test public void executedUnameMatchesProfileAndDisabledProfileClearsOldFunction() throws Exception {
        Map<String, String> p = properties();
        String first = executable(ShellIdentityScript.build(p, Collections.emptyMap()));
        Map<String, String> disabled = properties();
        disabled.put("identity.kernel.enabled", "false");
        String second = executable(ShellIdentityScript.build(disabled, Collections.emptyMap()));
        File inactive = temporary.newFile("inactive.sh");
        Files.write(inactive.toPath(), second.getBytes(StandardCharsets.UTF_8));
        String output = execute(first, "uname -rm\n. " + ShellIdentityScript.quote(posix(inactive)) + "\n"
            + "if [ \"$(command -v uname)\" = uname ]; then echo STALE; else echo CLEARED; fi\n");
        assertEquals("6.6-configured aarch64\nCLEARED\n", output);
    }

    private Map<String, String> properties() {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("ro.product.brand", "google");
        p.put("ro.product.model", "Pixel 10 Pro");
        p.put("ro.product.nickname", "Pixel 10 Pro");
        p.put("ro.soc.model", "Google Tensor G5");
        p.put("ro.soc.manufacturer", "Google");
        p.put("identity.kernel.enabled", "true");
        p.put("identity.kernel.release", "6.6-configured");
        p.put("identity.kernel.version", "#1 configured build");
        p.put("identity.kernel.machine", "aarch64");
        return p;
    }

    private static String posix(File file) {
        String path = file.getAbsolutePath().replace('\\', '/');
        return path.matches("^[A-Za-z]:/.*") ? "/" + Character.toLowerCase(path.charAt(0)) + path.substring(2) : path;
    }

    private static String executable(String script) {
        // The generated Android program runs unchanged apart from its external command paths.
        return script.replace("command /system/bin/getprop", "__fixture_getprop")
            .replace("command /system/bin/uname", "__fixture_uname").replace("/system/bin/", "");
    }

    private String execute(String script, String commands) throws Exception {
        String shell = System.getProperty("os.name").toLowerCase().contains("windows")
            ? "C:/Program Files/Git/bin/bash.exe" : "/bin/sh";
        Assume.assumeTrue(new File(shell).isFile());
        File program = new File(temporary.getRoot(), "identity.sh");
        File harness = new File(temporary.getRoot(), "harness.sh");
        Files.write(program.toPath(), executable(script).getBytes(StandardCharsets.UTF_8));
        String fixture = "__fixture_getprop() {\nif [ \"$#\" -eq 0 ]; then\n"
            + "printf '%s\\n' '[ro.product.model]: [iQOO Z10 Turbo+]' '[sys.boot_completed]: [1]' '[init.svc.bootanim]: [stopped]' '[persist.vivo.test]: [physical]' '[ro.vendor.mediatek.version.build.branch]: [physical_alps]'\n"
            + "else printf 'ORIGINAL\\n'; fi\n}\n__fixture_uname() { printf 'ORIGINAL\\n'; }\n"
            + ". ./identity.sh\n" + commands;
        Files.write(harness.toPath(), fixture.getBytes(StandardCharsets.UTF_8));
        Process process = new ProcessBuilder(shell, "harness.sh").directory(temporary.getRoot())
            .redirectErrorStream(true).start();
        try {
            assertTrue("Shell test deadline", process.waitFor(10, TimeUnit.SECONDS));
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(output, 0, process.exitValue());
            return output;
        } finally { process.destroyForcibly(); }
    }
}
