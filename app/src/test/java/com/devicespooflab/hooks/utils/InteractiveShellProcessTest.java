package com.devicespooflab.hooks.utils;

import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public class InteractiveShellProcessTest {
    @Test public void shellProtocolAndLifetimeStayIntactAcrossProfileChanges() throws Exception {
        Stub process = new Stub(); AtomicReference<String> script = new AtomicReference<>("/cache/first.sh");
        InteractiveShellProcess wrapped = new InteractiveShellProcess(process, script::get);
        wrapped.getOutputStream().write("getprop ro.product.model\necho END\n".getBytes(StandardCharsets.UTF_8));
        wrapped.getOutputStream().write("cat /proc/version\n".getBytes(StandardCharsets.UTF_8));
        script.set("/cache/second.sh"); wrapped.getOutputStream().write("uname -r\n".getBytes(StandardCharsets.UTF_8));
        script.set(""); wrapped.getOutputStream().write("getprop sys.boot_completed\n".getBytes(StandardCharsets.UTF_8));
        String sent = process.input.toString("UTF-8");
        assertEquals(1, sent.split("first.sh", -1).length - 1);
        assertEquals(1, sent.split("second.sh", -1).length - 1);
        assertTrue(sent.contains("echo END\n")); assertTrue(sent.contains(ShellIdentityScript.RESET));
        assertSame(process.stdout, wrapped.getInputStream()); assertSame(process.stderr, wrapped.getErrorStream());
        assertEquals(7, wrapped.waitFor()); wrapped.destroyForcibly(); assertFalse(process.alive);
    }
    @Test public void profileReloadNeverSplitsACommandAndEmptyWritesDoNothing() throws Exception {
        Stub process = new Stub();
        AtomicReference<String> script = new AtomicReference<>("/cache/first.sh");
        InteractiveShellProcess wrapped = new InteractiveShellProcess(process, script::get);
        wrapped.getOutputStream().write(new byte[0]);
        assertEquals(0, process.input.size());
        wrapped.getOutputStream().write("cat /proc/".getBytes(StandardCharsets.UTF_8));
        script.set("/cache/second.sh");
        wrapped.getOutputStream().write("version\n".getBytes(StandardCharsets.UTF_8));
        assertFalse(process.input.toString("UTF-8").contains("second.sh"));
        wrapped.getOutputStream().write("echo END\n".getBytes(StandardCharsets.UTF_8));
        String sent = process.input.toString("UTF-8");
        assertTrue(sent.contains("cat /proc/version\n"));
        assertTrue(sent.indexOf("second.sh") > sent.indexOf("cat /proc/version\n"));
    }
    private static final class Stub extends Process {
        final ByteArrayOutputStream input = new ByteArrayOutputStream();
        final InputStream stdout = new ByteArrayInputStream("result\nEND\n".getBytes(StandardCharsets.UTF_8));
        final InputStream stderr = new ByteArrayInputStream(new byte[0]); boolean alive = true;
        public OutputStream getOutputStream() { return input; } public InputStream getInputStream() { return stdout; }
        public InputStream getErrorStream() { return stderr; } public int waitFor() { return 7; }
        public int exitValue() { return 7; } public boolean isAlive() { return alive; } public void destroy() { alive = false; }
        public Process destroyForcibly() { alive = false; return this; }
    }
}
