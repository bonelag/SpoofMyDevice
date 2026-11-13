package com.devicespooflab.hooks.utils;

import static org.junit.Assert.*;
import java.lang.reflect.Modifier;
import org.junit.Test;

public class RuntimeInitializerPolicyTest {
    @Test public void recognizesRecordedShadowHookInitializersBeforeAndAfterR8() {
        int wrapper = Modifier.PUBLIC | Modifier.STATIC | Modifier.SYNCHRONIZED;
        assertTrue(RuntimeInitializerPolicy.isInitializer("init", wrapper, int.class, 1));
        assertTrue(RuntimeInitializerPolicy.isInitializer("a", wrapper, int.class, 1));
        assertTrue(RuntimeInitializerPolicy.isInitializer("nativeInit",
            Modifier.PRIVATE | Modifier.STATIC | Modifier.NATIVE, int.class, 2));
    }

    @Test public void nativeGettersAndOrdinaryApplicationMethodsDoNotAcquireRuntimeScopes() {
        int nativeMethod = Modifier.PRIVATE | Modifier.STATIC | Modifier.NATIVE;
        assertFalse(RuntimeInitializerPolicy.isInitializer("nativeGetInitErrno", nativeMethod, int.class, 0));
        assertFalse(RuntimeInitializerPolicy.isInitializer("nativeGetMode", nativeMethod, int.class, 0));
        assertFalse(RuntimeInitializerPolicy.isInitializer("a", Modifier.PUBLIC | Modifier.STATIC, int.class, 1));
        assertFalse(RuntimeInitializerPolicy.isInitializer("init", Modifier.PUBLIC, int.class, 1));
        assertFalse(RuntimeInitializerPolicy.isInitializer("a",
            Modifier.PUBLIC | Modifier.STATIC | Modifier.SYNCHRONIZED, String.class, 1));
        assertFalse(RuntimeInitializerPolicy.isLibraryClass("com.shopee.app.application.shopeetask.t2"));
    }
}
