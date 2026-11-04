package com.devicespooflab.hooks.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** Returning this process never waits for the child; stdout is rewritten one line at a time. */
public final class RewritingProcess extends Process {
    private final Process original;
    private final InputStream stdout;

    public RewritingProcess(Process original, OutputRewriter rewrite) {
        this.original = Objects.requireNonNull(original);
        stdout = new RewritingInputStream(original.getInputStream(), Objects.requireNonNull(rewrite));
    }

    public interface OutputRewriter {
        /** Includes the original line ending, if present. */
        String rewriteLine(String line);

        default String finish() { return ""; }
    }

    public static RewritingProcess replaceOutput(Process original, String output) {
        Objects.requireNonNull(output);
        return new RewritingProcess(original, new OutputRewriter() {
            private boolean emitted;

            @Override
            public String rewriteLine(String line) {
                return emitOnce();
            }

            @Override
            public String finish() {
                return emitOnce();
            }

            private String emitOnce() {
                if (emitted) {
                    return "";
                }
                emitted = true;
                return output;
            }
        });
    }

    @Override
    public OutputStream getOutputStream() { return original.getOutputStream(); }

    @Override
    public InputStream getInputStream() { return stdout; }

    @Override
    public InputStream getErrorStream() { return original.getErrorStream(); }

    @Override
    public int waitFor() throws InterruptedException { return original.waitFor(); }

    @Override
    public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
        return original.waitFor(timeout, unit);
    }

    @Override
    public int exitValue() { return original.exitValue(); }

    @Override
    public boolean isAlive() { return original.isAlive(); }

    @Override
    public void destroy() { original.destroy(); }

    @Override
    public Process destroyForcibly() {
        original.destroyForcibly();
        return this;
    }

    private static final class RewritingInputStream extends InputStream {
        private static final int MAX_LINE_BYTES = 64 * 1024;
        private final InputStream source;
        private final OutputRewriter rewrite;
        private InputStream rewritten = new ByteArrayInputStream(new byte[0]);
        private boolean sourceEnded;
        private boolean passthroughLine;
        private volatile boolean closed;

        private RewritingInputStream(InputStream source, OutputRewriter rewrite) {
            this.source = source;
            this.rewrite = rewrite;
        }

        private InputStream ready() throws IOException {
            ensureOpen();
            while (rewritten.available() == 0 && !sourceEnded) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                boolean completeLine = false;
                while (bytes.size() < MAX_LINE_BYTES) {
                    int next = source.read();
                    ensureOpen();
                    if (next == -1) {
                        sourceEnded = true;
                        break;
                    }
                    bytes.write(next);
                    if (next == '\n') {
                        completeLine = true;
                        break;
                    }
                }

                byte[] originalBytes = bytes.toByteArray();
                byte[] output = originalBytes;
                if (!passthroughLine && (completeLine || sourceEnded)) {
                    if (originalBytes.length != 0) {
                        String line = new String(originalBytes, StandardCharsets.UTF_8);
                        String replacement = Objects.requireNonNull(rewrite.rewriteLine(line));
                        if (!line.equals(replacement)) {
                            output = replacement.getBytes(StandardCharsets.UTF_8);
                        }
                    }
                }
                // Bound memory and let the caller read a long line before its producer exits.
                passthroughLine = !completeLine && !sourceEnded;
                if (sourceEnded) {
                    byte[] tail = Objects.requireNonNull(rewrite.finish()).getBytes(StandardCharsets.UTF_8);
                    ByteArrayOutputStream combined = new ByteArrayOutputStream();
                    combined.write(output, 0, output.length);
                    combined.write(tail, 0, tail.length);
                    output = combined.toByteArray();
                }
                rewritten = new ByteArrayInputStream(output);
            }
            return rewritten;
        }

        private void ensureOpen() throws IOException {
            if (closed) {
                throw new IOException("Stream closed");
            }
        }

        @Override
        public int read() throws IOException { return ready().read(); }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            Objects.requireNonNull(bytes);
            if (offset < 0 || length < 0 || offset > bytes.length - length) {
                throw new IndexOutOfBoundsException();
            }
            return length == 0 ? 0 : ready().read(bytes, offset, length);
        }

        @Override
        public int available() throws IOException {
            ensureOpen();
            // Untransformed source bytes may be filtered or expanded, so do not advertise them.
            return rewritten.available();
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                source.close();
                rewritten.close();
            }
        }
    }
}
