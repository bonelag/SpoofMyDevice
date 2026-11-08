package com.devicespooflab.hooks.utils;

import static org.junit.Assert.*;
import org.junit.Test;

public class IdentityPropertyPolicyTest {
    private StackTraceElement frame(String name) { return new StackTraceElement(name, "read", "Source.java", 1); }

    @Test public void frameworkDriverReadsPreservePhysicalValues() {
        assertTrue(IdentityPropertyPolicy.preservePlatformCaller("ro.hardware.egl", new StackTraceElement[]{
            frame("com.devicespooflab.hooks.hooks.SystemPropertiesHooks"), frame("android.os.SystemProperties"),
            frame("android.graphics.GraphicsEnvironment"), frame("com.example.App")}));
        assertTrue(IdentityPropertyPolicy.preservePlatformCaller("ro.product.cpu.abilist", new StackTraceElement[]{
            frame("android.os.SystemProperties"), frame("libcore.io.NativeLoader")}));
    }

    @Test public void reflectedAppReadsStillUseProfileValues() {
        assertFalse(IdentityPropertyPolicy.preservePlatformCaller("ro.board.platform", new StackTraceElement[]{
            frame("java.lang.Thread"), frame("de.robv.android.xposed.XposedBridge"), frame("android.os.SystemProperties"),
            frame("java.lang.reflect.Method"), frame("b1.S")}));
        assertFalse(IdentityPropertyPolicy.preservePlatformCaller("ro.hardware.vulkan", new StackTraceElement[]{
            frame("org.lsposed.lspd.NativeBridge"), frame("androidx.example.Reader")}));
        assertFalse(IdentityPropertyPolicy.preservePlatformCaller("ro.hardware.egl", new StackTraceElement[]{
            frame("android.os.SemSystemProperties"), frame("b1.S")}));
    }

    @Test public void ordinaryIdentityFieldsDoNotNeedCallerFiltering() {
        assertFalse(IdentityPropertyPolicy.preservePlatformCaller("ro.product.model", new StackTraceElement[]{
            frame("android.app.Application")}));
        assertFalse(IdentityPropertyPolicy.sensitive("sys.boot_completed"));
    }

    @Test public void unknownCallerFailsClosedOnlyForDriverProperties() {
        assertTrue(IdentityPropertyPolicy.preservePlatformCaller("ro.vendor.mediatek.platform", new StackTraceElement[0]));
        assertFalse(IdentityPropertyPolicy.preservePlatformCaller("ro.product.nickname", new StackTraceElement[0]));
    }
}
