package com.devicespooflab.hooks.utils;

import static org.junit.Assert.*;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public class RuntimeApiLevelPolicyTest {
    @Test public void higherSdkCannotSelectApi36MethodsOnApi35() {
        Map<String, String> requested = new LinkedHashMap<>();
        requested.put("ro.build.version.sdk", "36");
        requested.put("ro.vendor.build.version.sdk", "37");
        requested.put("ro.build.version.release", "16");
        requested.put("ro.product.model", "Pixel 10 Pro");
        Map<String, String> active = RuntimeApiLevelPolicy.limit(requested, 35);
        assertEquals("35", active.get("ro.build.version.sdk"));
        assertEquals("35", active.get("ro.vendor.build.version.sdk"));
        assertEquals("16", active.get("ro.build.version.release"));
        assertEquals("Pixel 10 Pro", active.get("ro.product.model"));
        assertEquals("36", requested.get("ro.build.version.sdk"));
    }

    @Test public void supportedLowerSdkAndUnassignmentRemainUnchanged() {
        assertEquals("33", RuntimeApiLevelPolicy.limit(java.util.Collections.singletonMap("ro.build.version.sdk", "33"), 35)
            .get("ro.build.version.sdk"));
        assertTrue(RuntimeApiLevelPolicy.limit(java.util.Collections.emptyMap(), 35).isEmpty());
    }

    @Test public void malformedSdkFallsBackWithoutChangingOtherMetadata() {
        assertEquals("35", RuntimeApiLevelPolicy.limit(java.util.Collections.singletonMap("ro.build.version.sdk", "invalid"), 35)
            .get("ro.build.version.sdk"));
    }

    @Test public void cappedAndMatchingSdksDoNotNeedGlobalRuntimeDispatchHooks() {
        Map<String, String> effective = RuntimeApiLevelPolicy.limit(java.util.Collections.singletonMap("ro.build.version.sdk", "37"), 35);
        assertFalse(RuntimeApiLevelPolicy.needsRuntimeSdkScope(Integer.parseInt(effective.get("ro.build.version.sdk")), 35));
        assertFalse(RuntimeApiLevelPolicy.needsRuntimeSdkScope(35, 35));
        assertFalse(RuntimeApiLevelPolicy.needsRuntimeSdkScope(0, 35));
    }

    @Test public void matchingSdkAvoidsWritesButLowerSdkStillProtectsRuntimeSelection() {
        java.util.concurrent.atomic.AtomicInteger sdk = new java.util.concurrent.atomic.AtomicInteger(35);
        java.util.concurrent.atomic.AtomicInteger writes = new java.util.concurrent.atomic.AtomicInteger();
        RuntimeSdkScope scope = new RuntimeSdkScope(35, sdk::get, value -> { writes.incrementAndGet(); sdk.set(value); }, enabled -> {});
        if (RuntimeApiLevelPolicy.needsRuntimeSdkScope(sdk.get(), 35)) {
            try (RuntimeSdkScope.Lease ignored = scope.enter()) { fail("Matching SDK must not open a runtime scope"); }
        }
        assertEquals(0, writes.get());
        assertFalse(RuntimeSdkScope.active());
        sdk.set(33);
        assertTrue(RuntimeApiLevelPolicy.needsRuntimeSdkScope(sdk.get(), 35));
        try (RuntimeSdkScope.Lease ignored = scope.enter()) {
            assertEquals(35, sdk.get());
            assertTrue(RuntimeSdkScope.active());
        }
        assertEquals(33, sdk.get());
        assertFalse(RuntimeSdkScope.active());
    }
}
