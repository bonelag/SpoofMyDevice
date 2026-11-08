package com.devicespooflab.hooks.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** Preserves shell protocol/output/lifecycle; installs identity functions before stdin commands. */
public final class InteractiveShellProcess extends Process {
    private final Process original;
    private final OutputStream input;
    public InteractiveShellProcess(Process original, Supplier<String> scriptPath) {
        this.original = Objects.requireNonNull(original);
        OutputStream destination = original.getOutputStream();
        input = new OutputStream() {
            private String loaded;
            private boolean atLineStart = true;
            private void prepare() throws IOException {
                if (!atLineStart) return;
                String selected = scriptPath.get();
                if (selected == null) selected = "";
                if (selected.equals(loaded)) return;
                String command = selected.isEmpty() ? (loaded == null ? "" : ShellIdentityScript.RESET)
                    : ". " + ShellIdentityScript.quote(selected) + "\n";
                destination.write(command.getBytes(StandardCharsets.UTF_8));
                loaded = selected;
            }
            @Override public synchronized void write(int value) throws IOException {
                prepare(); destination.write(value); atLineStart = value == '\n';
            }
            @Override public synchronized void write(byte[] bytes, int offset, int count) throws IOException {
                Objects.requireNonNull(bytes);
                if (offset < 0 || count < 0 || offset > bytes.length - count) throw new IndexOutOfBoundsException();
                if (count == 0) return;
                prepare(); destination.write(bytes, offset, count); atLineStart = bytes[offset + count - 1] == '\n';
            }
            @Override public synchronized void flush() throws IOException { destination.flush(); }
            @Override public synchronized void close() throws IOException { destination.close(); }
        };
    }
    @Override public OutputStream getOutputStream() { return input; }
    @Override public InputStream getInputStream() { return original.getInputStream(); }
    @Override public InputStream getErrorStream() { return original.getErrorStream(); }
    @Override public int waitFor() throws InterruptedException { return original.waitFor(); }
    @Override public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException { return original.waitFor(timeout, unit); }
    @Override public int exitValue() { return original.exitValue(); }
    @Override public boolean isAlive() { return original.isAlive(); }
    @Override public void destroy() { original.destroy(); }
    @Override public Process destroyForcibly() { original.destroyForcibly(); return this; }
}
