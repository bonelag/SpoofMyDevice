package com.devicespooflab.hooks.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class RewritingProcessTest {
    @Test
    public void processCanBeReturnedAndCancelledWithoutReadingOrWaiting() throws Exception {
        InputStream blocked = new InputStream() {
            @Override
            public int read() { throw new AssertionError("stdout read before the caller consumed it"); }
        };
        ProbeProcess child = new ProbeProcess(blocked);
        Process wrapper = new RewritingProcess(child, output -> "spoofed\n");
        assertSame(wrapper.getInputStream(), wrapper.getInputStream());
        assertSame(child.stderr, wrapper.getErrorStream());
        assertSame(child.stdin, wrapper.getOutputStream());
        assertEquals(0, child.waitCalls);
        assertTrue(wrapper.isAlive());
        assertThrows(IllegalThreadStateException.class, wrapper::exitValue);
        assertFalse(wrapper.waitFor(1, TimeUnit.MILLISECONDS));
        assertEquals(1, child.waitCalls);
        wrapper.destroy();
        assertFalse(child.alive);
    }

    @Test
    public void stdoutRewriteKeepsStderrAndActualExitStatus() throws Exception {
        ProbeProcess child = new ProbeProcess(new ByteArrayInputStream("real\n".getBytes(StandardCharsets.UTF_8)));
        Process wrapper = new RewritingProcess(child, output -> output.replace("real", "spoofed"));
        assertEquals("spoofed\n", new String(wrapper.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        assertEquals("warning\n", new String(wrapper.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
        assertEquals(7, wrapper.waitFor());
        assertEquals(7, wrapper.exitValue());
    }

    @Test
    public void stdoutReadFailureIsVisibleToTheCaller() {
        InputStream broken = new InputStream() {
            @Override
            public int read() throws IOException { throw new IOException("child stdout failure"); }
        };
        Process wrapper = new RewritingProcess(new ProbeProcess(broken), output -> output);
        IOException failure = assertThrows(IOException.class, () -> wrapper.getInputStream().read());
        assertEquals("child stdout failure", failure.getMessage());
    }

    @Test
    public void completedLineIsReadableBeforeChildClosesStdout() throws Exception {
        PipedInputStream source = new PipedInputStream();
        PipedOutputStream producer = new PipedOutputStream(source);
        ExecutorService reader = Executors.newSingleThreadExecutor();
        Process wrapper = new RewritingProcess(new ProbeProcess(source), line -> line.replace("real", "spoofed"));
        try {
            producer.write("real\n".getBytes(StandardCharsets.UTF_8));
            Future<String> firstLine = reader.submit(() -> {
                byte[] buffer = new byte[64];
                int count = wrapper.getInputStream().read(buffer);
                return new String(buffer, 0, count, StandardCharsets.UTF_8);
            });
            assertEquals("spoofed\n", firstLine.get(1, TimeUnit.SECONDS));
            producer.close();
            assertEquals(-1, wrapper.getInputStream().read());
        } finally {
            producer.close();
            source.close();
            reader.shutdownNow();
        }
    }

    @Test
    public void oversizedUnterminatedLineCanBeReadBeforeChildExits() throws Exception {
        byte[] output = new byte[64 * 1024];
        java.util.Arrays.fill(output, (byte) 'x');
        PipedInputStream source = new PipedInputStream(output.length + 1);
        PipedOutputStream producer = new PipedOutputStream(source);
        ExecutorService reader = Executors.newSingleThreadExecutor();
        Process wrapper = new RewritingProcess(new ProbeProcess(source), line -> "rewritten\n");
        try {
            producer.write(output);
            Future<Integer> firstChunk = reader.submit(() -> wrapper.getInputStream().read(new byte[output.length]));
            assertEquals(output.length, firstChunk.get(2, TimeUnit.SECONDS).intValue());
        } finally {
            producer.close();
            source.close();
            reader.shutdownNow();
        }
    }

    @Test
    public void fixedOutputIsEmittedOnceAndRemainingStdoutIsConsumed() throws Exception {
        ProbeProcess child = new ProbeProcess(new ByteArrayInputStream("one\ntwo\nthree\n".getBytes(StandardCharsets.UTF_8)));
        Process wrapper = RewritingProcess.replaceOutput(child, "replacement\n");
        assertEquals("replacement\n", new String(wrapper.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        assertSame(wrapper, wrapper.destroyForcibly());
        assertFalse(child.alive);
    }

    private static final class ProbeProcess extends Process {
        private final InputStream stdout;
        private final InputStream stderr = new ByteArrayInputStream("warning\n".getBytes(StandardCharsets.UTF_8));
        private final ByteArrayOutputStream stdin = new ByteArrayOutputStream();
        private boolean alive = true;
        private int waitCalls;

        private ProbeProcess(InputStream stdout) { this.stdout = stdout; }
        @Override public OutputStream getOutputStream() { return stdin; }
        @Override public InputStream getInputStream() { return stdout; }
        @Override public InputStream getErrorStream() { return stderr; }
        @Override public int waitFor() { waitCalls++; alive = false; return 7; }
        @Override public boolean waitFor(long timeout, TimeUnit unit) { waitCalls++; return !alive; }
        @Override public int exitValue() {
            if (alive) throw new IllegalThreadStateException("child still running");
            return 7;
        }
        @Override public boolean isAlive() { return alive; }
        @Override public void destroy() { alive = false; }
    }
}
