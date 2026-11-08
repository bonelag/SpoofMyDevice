package com.devicespooflab.hooks.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class RuntimeConfigStoreTest {
    @Test
    public void hookReadersContinueWhileConfigurationTransportIsBlocked() throws Exception {
        RuntimeConfigStore store = new RuntimeConfigStore(snapshot("Before"));
        CountDownLatch loading = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService worker = Executors.newSingleThreadExecutor();
        ExecutorService reader = Executors.newSingleThreadExecutor();
        try {
            Future<?> reload = worker.submit(() -> store.reload(() -> {
                loading.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("Test did not release the blocked loader");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(exception);
                }
                return snapshot("After");
            }));
            assertTrue(loading.await(2, TimeUnit.SECONDS));
            Future<String> value = reader.submit(() -> store.current().properties.get("ro.product.model"));
            assertEquals("Before", value.get(1, TimeUnit.SECONDS));
            release.countDown();
            reload.get(2, TimeUnit.SECONDS);
            assertEquals("After", store.current().properties.get("ro.product.model"));
        } finally {
            release.countDown();
            worker.shutdownNow();
            reader.shutdownNow();
        }
    }

    @Test
    public void failedReloadDoesNotDestroyThePublishedProfile() {
        RuntimeConfigStore store = new RuntimeConfigStore(snapshot("Before"));
        assertThrows(IllegalStateException.class, () -> store.reload(() -> {
            throw new IllegalStateException("Provider unavailable");
        }));
        assertEquals("Before", store.current().properties.get("ro.product.model"));
        assertTrue(store.current().hasAssignedProfile());
    }

    @Test
    public void publishedDataCannotChangeUnderHookReaders() {
        Map<String, String> mutable = new HashMap<>();
        mutable.put("ro.product.model", "Before");
        RuntimeConfigStore store = new RuntimeConfigStore(new RuntimeConfigStore.Snapshot(mutable, false));
        mutable.put("ro.product.model", "After");
        assertEquals("Before", store.current().properties.get("ro.product.model"));
        assertThrows(UnsupportedOperationException.class,
            () -> store.current().properties.put("ro.product.model", "Changed"));
    }

    @Test
    public void startupIdentityIsReadableWhileVirtualFilesAreStillBeingPrepared() throws Exception {
        RuntimeConfigStore store = new RuntimeConfigStore(new RuntimeConfigStore.Snapshot(Collections.emptyMap(), true));
        CountDownLatch preparing = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> load = worker.submit(() -> store.reloadForStartup(() -> snapshot("Pixel"), next -> {
                preparing.countDown();
                try { release.await(); }
                catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IllegalStateException(error); }
            }));
            assertTrue(preparing.await(2, TimeUnit.SECONDS));
            assertEquals("Pixel", store.current().properties.get("ro.product.model"));
            assertTrue(store.current().hasAssignedProfile());
            assertFalse(StartupConfigLoad.await(load));
            release.countDown();
            assertTrue(load.get(2, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            worker.shutdownNow();
        }
    }

    @Test
    public void failedStartupAndLiveSnapshotsDoNotUseEarlyPublication() {
        RuntimeConfigStore store = new RuntimeConfigStore(new RuntimeConfigStore.Snapshot(Collections.emptyMap(), true));
        assertFalse(store.reloadForStartup(() -> null, next -> { throw new AssertionError("No snapshot to prepare"); }));
        assertFalse(store.current().hasAssignedProfile());
        store.reload(() -> snapshot("Assigned"));
        assertFalse(store.reloadForStartup(() -> { throw new AssertionError("Not a startup snapshot"); }, next -> {}));
        assertEquals("Assigned", store.current().properties.get("ro.product.model"));
    }

    private static RuntimeConfigStore.Snapshot snapshot(String model) {
        return new RuntimeConfigStore.Snapshot(Collections.singletonMap("ro.product.model", model), false);
    }
}
