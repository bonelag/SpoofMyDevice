package com.devicespooflab.hooks.utils;

import android.content.Context;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** The configuration worker prepares virtual files; hook callbacks only read this view. */
public final class ScopedHardwareRuntime {
    private static volatile View view = new View(Collections.emptyMap(), Collections.emptyMap(), "", "");

    public static final class View {
        public final Map<String, String> paths;
        public final Map<String, String> properties;
        public final String generation;
        public final String shellScript;
        private final IdentityPaths.Index pathIndex;
        private View(Map<String, String> paths, Map<String, String> properties, String generation, String shellScript) {
            pathIndex = new IdentityPaths.Index(paths);
            this.paths = pathIndex.paths();
            this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
            this.generation = generation;
            this.shellScript = shellScript;
        }
        public String redirect(String path) {
            return pathIndex.redirect(path);
        }
    }

    private ScopedHardwareRuntime() { }
    public static View current() { return view; }

    /** Must run on a worker, before publishing the associated configuration snapshot. */
    public static void prepare(Context context, RuntimeConfigStore.Snapshot snapshot) {
        HardwareIdentity identity = snapshot.hardware;
        if (!snapshot.hasAssignedProfile()) {
            view = new View(Collections.emptyMap(), Collections.emptyMap(), "unassigned", "");
            NativeIdentity.publish(view.properties, view.paths);
            return;
        }
        if (context == null) return;
        try {
            String generation = digest(IdentityFileCache.FORMAT_VERSION + "\n" + snapshot.properties);
            if (generation.equals(view.generation)) return;
            File directory = new File(context.getCacheDir(), "smd_identity/" + generation);
            Map<String, String> properties = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : snapshot.properties.entrySet()) {
                String key = entry.getKey();
                if (HookSafetyPolicy.isRuntimeStateProperty(key)) continue;
                String field = ConfigManager.getToggleFieldForSystemProperty(key);
                if (field != null && !identity.enabled(field)) continue;
                String alias = identity.propertyOverride(key);
                properties.put(key, alias == null ? entry.getValue() : alias);
            }
            properties.putAll(identity.propertyAliases());
            String safePackages = snapshot.properties.getOrDefault(ConfigManager.KEY_SAFE_MODE_PACKAGES, "");
            for (String selected : safePackages.split("[,\\n]")) {
                if (selected.trim().equals(context.getPackageName())) {
                    properties.keySet().removeIf(key -> key.contains(".build.version."));
                    break;
                }
            }
            properties.put("identity.kernel.release", identity.kernelRelease());
            properties.put("identity.kernel.version", identity.kernelVersion());
            properties.put("identity.kernel.machine", identity.machine());
            properties.put("identity.kernel.hostname", identity.value("kernel.hostname", "localhost"));
            properties.put("identity.kernel.enabled", String.valueOf(identity.enabled("kernel")));
            properties.put("identity.sensor.enabled", String.valueOf(identity.enabled("sensor_metadata")));
            properties.put("identity.ram.enabled", String.valueOf(identity.enabled("ram")));
            properties.put("identity.ram.bytes", String.valueOf(identity.ramBytes));
            properties.put("identity.storage.bytes", identity.value("hardware.storage_bytes", "274877906944"));
            properties.put("identity.storage.enabled", String.valueOf(identity.enabled("storage_metadata")));
            properties.put("identity.soc.enabled", String.valueOf(identity.enabled("soc_model")));
            properties.put("identity.soc.manufacturer", identity.value("ro.soc.manufacturer", ""));
            for (int type = 1; type <= 40; type++) {
                properties.put("identity.sensor." + type + ".name", identity.sensorName(type));
                properties.put("identity.sensor." + type + ".vendor", identity.sensorVendor(type));
            }
            IdentityFileCache.Prepared cache = IdentityFileCache.prepare(directory, identity.files(), identity.directories(), properties);
            View next = new View(cache.paths, properties, generation, cache.script);
            // Native initialization can race the worker. Publish the Java view first so install()
            // either sees this view or is followed by nativePublish, never a permanently empty table.
            view = next;
            NativeIdentity.publish(next.properties, next.paths);
        } catch (Exception error) {
            // A failed preparation must not leave virtual paths from a previous identity active.
            view = new View(Collections.emptyMap(), Collections.emptyMap(), "unavailable", "");
            NativeIdentity.publish(view.properties, view.paths);
            android.util.Log.w("SpoofMyDevice", "Virtual identity preparation failed", error);
        }
    }

    private static String digest(String content) throws Exception {
        byte[] bytes = MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
        StringBuilder text = new StringBuilder();
        for (byte value : bytes) text.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
        return text.toString();
    }
}
