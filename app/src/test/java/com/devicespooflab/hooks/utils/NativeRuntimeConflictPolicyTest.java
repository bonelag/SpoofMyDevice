package com.devicespooflab.hooks.utils;

import static org.junit.Assert.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class NativeRuntimeConflictPolicyTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private static final String INITIALIZED = "smd.test.host.shadowhook.initialized";

    private URL compiledHostRuntime() throws Exception {
        Path root = temporary.newFolder().toPath();
        Path source = root.resolve("ShadowHook.java");
        Files.write(source, ("package com.bytedance.shadowhook; public final class ShadowHook {"
            + "static { System.setProperty(\"" + INITIALIZED + "\", \"yes\"); } }").getBytes(StandardCharsets.UTF_8));
        // Android's test compile classpath omits javax.tools; the host test JDK supplies it.
        Object compiler = Class.forName("javax.tools.ToolProvider").getMethod("getSystemJavaCompiler").invoke(null);
        Object result = Class.forName("javax.tools.Tool").getMethod("run", java.io.InputStream.class,
            java.io.OutputStream.class, java.io.OutputStream.class, String[].class).invoke(compiler,
                null, null, null, (Object) new String[]{"-d", root.toString(), source.toString()});
        assertEquals(0, ((Integer) result).intValue());
        return root.toUri().toURL();
    }

    @Test public void hostOwnedEngineIsDetectedWithoutRunningItsInitializer() throws Exception {
        System.clearProperty(INITIALIZED);
        try (URLClassLoader host = new URLClassLoader(new URL[]{compiledHostRuntime()}, null)) {
            assertTrue(NativeRuntimeConflictPolicy.hasHostShadowHook(host, getClass().getClassLoader()));
            assertNull("Class probing must leave host initialization to the application", System.getProperty(INITIALIZED));
        } finally { System.clearProperty(INITIALIZED); }
    }

    @Test public void parentDelegationToModuleOwnedEngineIsNotAConflict() throws Exception {
        try (URLClassLoader module = new URLClassLoader(new URL[]{compiledHostRuntime()}, null);
             URLClassLoader host = new URLClassLoader(new URL[0], module)) {
            assertFalse(NativeRuntimeConflictPolicy.hasHostShadowHook(host, module));
            assertFalse(NativeRuntimeConflictPolicy.hasHostShadowHook(module, module));
        }
    }

    @Test public void absentHostRuntimeAndBootstrapLoaderKeepNativeSupportAvailable() throws Exception {
        try (URLClassLoader host = new URLClassLoader(new URL[0], null)) {
            assertFalse(NativeRuntimeConflictPolicy.hasHostShadowHook(host, getClass().getClassLoader()));
            assertFalse(NativeRuntimeConflictPolicy.hasHostShadowHook(null, getClass().getClassLoader()));
        }
    }

    @Test public void incompleteHostRuntimeAvoidsIntroducingAnotherNativeEngine() {
        ClassLoader incomplete = new ClassLoader(null) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.equals("com.bytedance.shadowhook.ShadowHook")) throw new NoClassDefFoundError("Missing host dependency");
                return super.loadClass(name, resolve);
            }
        };
        assertTrue(NativeRuntimeConflictPolicy.hasHostShadowHook(incomplete, getClass().getClassLoader()));
    }
}
