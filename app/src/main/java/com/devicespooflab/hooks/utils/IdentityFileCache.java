package com.devicespooflab.hooks.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Worker-only, versioned cache. A complete generation can be reused across app processes. */
public final class IdentityFileCache {
    // Bump when generated file/script semantics change so old complete caches cannot be reused.
    public static final String FORMAT_VERSION = "2";
    private IdentityFileCache() { }

    public static final class Prepared {
        public final Map<String, String> paths;
        public final String script;
        private Prepared(Map<String, String> paths, String script) {
            this.paths = paths;
            this.script = script;
        }
    }

    public static Prepared prepare(File directory, Map<String, String> files,
        Map<String, List<String>> inventories, Map<String, String> properties) throws IOException {
        File complete = new File(directory, ".complete");
        File script = new File(directory, "identity.sh");
        boolean reusable = complete.isFile() && script.isFile();
        Map<String, String> redirects = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : files.entrySet()) {
            File target = new File(directory, entry.getKey().substring(1));
            if (!reusable || !target.isFile()) {
                makeDirectory(target.getParentFile());
                // An interrupted generation may contain partial files. Rebuild until complete.
                write(directory, target, entry.getValue().getBytes(StandardCharsets.UTF_8));
            }
            addParents(redirects, directory, entry.getKey());
            redirects.put(entry.getKey(), target.getAbsolutePath());
        }
        for (Map.Entry<String, List<String>> entry : inventories.entrySet()) {
            File target = new File(directory, entry.getKey().substring(1));
            if (!reusable || !target.isDirectory()) {
                makeDirectory(target);
                target.setReadable(true, false);
                target.setExecutable(true, false);
            }
            for (String item : entry.getValue()) {
                File child = new File(target, item);
                // Cache trimming can evict individual entries while the marker survives.
                if (!reusable || !child.exists()) {
                    if (!child.exists()) makeDirectory(child);
                    child.setReadable(true, false);
                    if (child.isDirectory()) child.setExecutable(true, false);
                }
            }
            redirects.put(entry.getKey(), target.getAbsolutePath());
        }
        redirects = IdentityPaths.compact(redirects);
        if (!reusable) {
            makeDirectory(directory);
            write(directory, script, ShellIdentityScript.build(properties, redirects).getBytes(StandardCharsets.UTF_8));
            // Publish completion only after every file, inventory and script is ready.
            write(directory, complete, new byte[0]);
        }
        return new Prepared(redirects, script.getAbsolutePath());
    }

    private static void write(File directory, File target, byte[] content) throws IOException {
        // Keep staging files outside exposed CPU/component inventories. Atomic replacement
        // lets other target processes read this generation without seeing a truncated file.
        Path temporary = Files.createTempFile(directory.toPath(), ".smd-", ".tmp");
        try {
            Files.write(temporary, content);
            temporary.toFile().setReadable(true, false);
            Files.move(temporary, target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void makeDirectory(File directory) throws IOException {
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Virtual identity directory creation failed");
    }

    private static void addParents(Map<String, String> paths, File base, String path) {
        String parent = path.substring(0, path.lastIndexOf('/'));
        // Do not replace /proc or /sys themselves: only identity subtrees are virtual.
        if (parent.startsWith("/sys/devices/system/cpu/") || parent.startsWith("/sys/class/sensors/")) {
            while (parent.length() > "/sys/devices/system/cpu".length()) {
                paths.put(parent, new File(base, parent.substring(1)).getAbsolutePath());
                int slash = parent.lastIndexOf('/');
                if (slash <= 0) break;
                parent = parent.substring(0, slash);
            }
            if (path.startsWith("/sys/devices/system/cpu/")) {
                paths.put("/sys/devices/system/cpu", new File(base, "sys/devices/system/cpu").getAbsolutePath());
            }
        }
    }
}
