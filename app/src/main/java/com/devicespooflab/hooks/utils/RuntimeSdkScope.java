package com.devicespooflab.hooks.utils;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Consumer;

/** Overlapping runtime calls share the actual SDK without holding a lock across their bodies. */
public final class RuntimeSdkScope {
    private static final AtomicInteger ACTIVE = new AtomicInteger();
    private final ReentrantLock lock = new ReentrantLock();
    private final int actual;
    private final IntSupplier read;
    private final IntConsumer write;
    private final Consumer<Boolean> nativeBypass;
    private int users;
    private int appSdk;

    public RuntimeSdkScope(int actual, IntSupplier read, IntConsumer write, Consumer<Boolean> nativeBypass) {
        this.actual = actual;
        this.read = read;
        this.write = write;
        this.nativeBypass = nativeBypass;
    }

    public static boolean active() { return ACTIVE.get() > 0; }

    public Lease enter() {
        lock.lock();
        try {
            if (users == 0) {
                int previous = read.getAsInt();
                ACTIVE.incrementAndGet();
                try {
                    nativeBypass.accept(true);
                    write.accept(actual);
                    appSdk = previous;
                } catch (RuntimeException | Error failure) {
                    try { write.accept(previous); }
                    finally { ACTIVE.decrementAndGet(); nativeBypass.accept(active()); }
                    throw failure;
                }
            } else {
                ACTIVE.incrementAndGet();
            }
            users++;
            return new Lease();
        } finally { lock.unlock(); }
    }

    public final class Lease implements AutoCloseable {
        private boolean closed;
        private Lease() { }
        @Override public void close() {
            lock.lock();
            try {
                if (closed) return;
                closed = true;
                users--;
                ACTIVE.decrementAndGet();
                if (users == 0) {
                    try { write.accept(appSdk); }
                    finally { nativeBypass.accept(active()); }
                }
            } finally { lock.unlock(); }
        }
    }
}
