package com.devicespooflab.hooks.utils;

import static org.junit.Assert.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class StartupConfigLoadTest {
    @Test public void blockedCacheHasADeadlineAndCanFinishLater() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        FutureTask<Boolean> task = new FutureTask<>(() -> { release.await(); return true; });
        Thread worker = new Thread(task);
        worker.start();
        try {
            assertFalse(StartupConfigLoad.await(task));
            assertFalse(task.isCancelled());
        } finally { release.countDown(); worker.join(2000); }
        assertTrue(task.get(1, TimeUnit.SECONDS));
        assertTrue(StartupConfigLoad.await(task));
    }
    @Test public void failedCacheDoesNotEscapeIntoApplicationStartup() {
        FutureTask<Boolean> task = new FutureTask<>(() -> { throw new IllegalStateException("Unavailable cache"); });
        task.run(); assertFalse(StartupConfigLoad.await(task));
    }
}
