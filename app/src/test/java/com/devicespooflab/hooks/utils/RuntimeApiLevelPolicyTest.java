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
}
