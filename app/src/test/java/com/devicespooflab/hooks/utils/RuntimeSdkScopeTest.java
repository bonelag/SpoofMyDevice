package com.devicespooflab.hooks.utils;

import static org.junit.Assert.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;

public class RuntimeSdkScopeTest {
    @Test public void nativeInitializerCachesPhysicalApiWithoutChangingLowerAppSdk() {
        AtomicInteger appSdk = new AtomicInteger(34);
        AtomicInteger cachedNativeApi = new AtomicInteger(-1);
        AtomicBoolean nativeBypass = new AtomicBoolean();
        RuntimeSdkScope scope = new RuntimeSdkScope(35, appSdk::get, appSdk::set, nativeBypass::set);
        try (RuntimeSdkScope.Lease wrapper = scope.enter()) {
            try (RuntimeSdkScope.Lease nativeInit = scope.enter()) {
                // NDK's pre-29 inline API discovery reads the property through native hooks.
                cachedNativeApi.set(nativeBypass.get() ? 35 : appSdk.get());
                assertEquals(35, appSdk.get());
            }
            assertTrue(nativeBypass.get());
        }
        assertEquals(35, cachedNativeApi.get());
        assertEquals(34, appSdk.get());
        assertFalse(nativeBypass.get());
        assertFalse(RuntimeSdkScope.active());
    }

    @Test public void runtimeUsesActualSdkAndAppValueReturnsAfterNestedCalls() {
        AtomicInteger sdk = new AtomicInteger(36);
        AtomicBoolean nativeBypass = new AtomicBoolean();
        RuntimeSdkScope scope = new RuntimeSdkScope(35, sdk::get, sdk::set, nativeBypass::set);
        try (RuntimeSdkScope.Lease outer = scope.enter()) {
            assertEquals(35, sdk.get());
            assertTrue(nativeBypass.get());
            try (RuntimeSdkScope.Lease nested = scope.enter()) { assertEquals(35, sdk.get()); }
            assertEquals(35, sdk.get());
            assertTrue(RuntimeSdkScope.active());
        }
        assertEquals(36, sdk.get());
        assertFalse(nativeBypass.get());
        assertFalse(RuntimeSdkScope.active());
    }

    @Test public void exceptionAndDuplicateCloseDoNotLeaveSdkOrBypassChanged() {
        AtomicInteger sdk = new AtomicInteger(37);
        AtomicBoolean bypass = new AtomicBoolean();
        RuntimeSdkScope scope = new RuntimeSdkScope(35, sdk::get, sdk::set, bypass::set);
        try (RuntimeSdkScope.Lease lease = scope.enter()) { throw new IllegalStateException("runtime failed"); }
        catch (IllegalStateException expected) { }
        assertEquals(37, sdk.get());
        RuntimeSdkScope.Lease lease = scope.enter();
        lease.close(); lease.close();
        assertEquals(37, sdk.get());
        assertFalse(bypass.get());
        assertFalse(RuntimeSdkScope.active());
    }

    @Test public void failedEntryRestoresStateAndDoesNotKeepTheLock() throws Exception {
        AtomicInteger sdk = new AtomicInteger(36);
        AtomicBoolean failRead = new AtomicBoolean(true);
        RuntimeSdkScope scope = new RuntimeSdkScope(35, () -> {
            if (failRead.getAndSet(false)) throw new IllegalStateException("read failed");
            return sdk.get();
        }, sdk::set, enabled -> {});
        assertThrows(IllegalStateException.class, scope::enter);
        java.util.concurrent.ExecutorService worker = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            assertEquals(Integer.valueOf(35), worker.submit(() -> {
                try (RuntimeSdkScope.Lease lease = scope.enter()) { return sdk.get(); }
            }).get(2, java.util.concurrent.TimeUnit.SECONDS));
        } finally { worker.shutdownNow(); }
        assertEquals(36, sdk.get());
        assertFalse(RuntimeSdkScope.active());
    }

    @Test public void overlappingRuntimeThreadsDoNotBlockAndOnlyLastExitRestoresAppSdk() throws Exception {
        AtomicInteger sdk = new AtomicInteger(36);
        AtomicBoolean bypass = new AtomicBoolean();
        RuntimeSdkScope scope = new RuntimeSdkScope(35, sdk::get, sdk::set, bypass::set);
        RuntimeSdkScope.Lease main = scope.enter();
        java.util.concurrent.ExecutorService worker = java.util.concurrent.Executors.newSingleThreadExecutor();
        java.util.concurrent.CountDownLatch entered = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        try {
            java.util.concurrent.Future<?> task = worker.submit(() -> {
                try (RuntimeSdkScope.Lease background = scope.enter()) {
                    assertEquals(35, sdk.get());
                    entered.countDown();
                    try { release.await(); } catch (InterruptedException error) { throw new IllegalStateException(error); }
                }
            });
            assertTrue("Runtime worker must enter while the caller is active",
                entered.await(2, java.util.concurrent.TimeUnit.SECONDS));
            main.close();
            assertEquals(35, sdk.get());
            assertTrue(bypass.get());
            release.countDown();
            task.get(2, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(36, sdk.get());
            assertFalse(bypass.get());
        } finally { main.close(); release.countDown(); worker.shutdownNow(); }
        assertFalse(RuntimeSdkScope.active());
    }
}
