package com.devicespooflab.hooks.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HookSafetyPolicyTest {
    @Test
    public void softRebootMustNotReuseCompletedBootState() {
        assertTrue(HookSafetyPolicy.isBootReady("1", "stopped"));
        assertFalse(HookSafetyPolicy.isBootReady("1", "running"));
        assertFalse(HookSafetyPolicy.isBootReady("1", "restarting"));
        assertFalse(HookSafetyPolicy.isBootReady("0", "stopped"));
        assertTrue(HookSafetyPolicy.isBootReady("1", "stopped"));
    }

    @Test
    public void missingOrUnknownBootStateKeepsRealValues() {
        assertFalse(HookSafetyPolicy.isBootReady(null, "stopped"));
        assertFalse(HookSafetyPolicy.isBootReady("", "stopped"));
        assertFalse(HookSafetyPolicy.isBootReady("2", "stopped"));
        assertFalse(HookSafetyPolicy.isBootReady("true", "stopped"));
        assertFalse(HookSafetyPolicy.isBootReady("1", null));
        assertFalse(HookSafetyPolicy.isBootReady("1", ""));
    }

    @Test
    public void systemAppsAndSecondaryUserSystemUidsAreProtected() {
        assertTrue(HookSafetyPolicy.isProtectedProcess("com.example.service", "com.example.service", 10077, 1));
        assertTrue(HookSafetyPolicy.isProtectedProcess("com.example.service", "com.example.service", 10077, 128));
        assertTrue(HookSafetyPolicy.isProtectedUid(1000));
        assertTrue(HookSafetyPolicy.isProtectedUid(101000));
        assertTrue(HookSafetyPolicy.isProtectedUid(99001));
        assertTrue(HookSafetyPolicy.isProtectedUid(-1));
        assertFalse(HookSafetyPolicy.isProtectedProcess("com.example.app", "com.example.app", 110123, 0));
        assertTrue(HookSafetyPolicy.isProtectedProcess("com.example.app", "zygote64", 10123, 0));
    }

    @Test
    public void profilesCannotOverrideRuntimeBootControlProperties() {
        assertTrue(HookSafetyPolicy.isRuntimeStateProperty("sys.boot_completed"));
        assertTrue(HookSafetyPolicy.isRuntimeStateProperty("init.svc.bootanim"));
        assertTrue(HookSafetyPolicy.isRuntimeStateProperty("init.svc.zygote"));
        assertTrue(HookSafetyPolicy.isRuntimeStateProperty("service.bootanim.exit"));
        assertFalse(HookSafetyPolicy.isRuntimeStateProperty("ro.build.fingerprint"));
        assertFalse(HookSafetyPolicy.isRuntimeStateProperty("ro.boot.serialno"));
    }
}
