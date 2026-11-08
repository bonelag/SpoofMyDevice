package com.devicespooflab.hooks.utils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/** POSIX identity paths must have identical semantics on Android, in shells and in JVM tests. */
public final class IdentityPaths {
    private IdentityPaths() { }

    public static String normalize(String path) {
        if (path == null || !path.startsWith("/") || path.indexOf('\0') >= 0) return null;
        Deque<String> parts = new ArrayDeque<>();
        for (String part : path.split("/+")) {
            if (part.isEmpty() || part.equals(".")) continue;
            if (part.equals("..")) {
                if (!parts.isEmpty()) parts.removeLast();
            } else parts.addLast(part);
        }
        return "/" + String.join("/", parts);
    }

    public static String redirect(Map<String, String> paths, String path) {
        String normalized = normalize(path);
        if (normalized == null) return null;
        String exact = paths.get(normalized);
        if (exact != null) return exact;
        String best = null;
        for (String logical : paths.keySet()) {
            // A proc file is not a directory. Directory inventories in procfs are explicit roots.
            if (logical.startsWith("/proc/") && !logical.startsWith("/proc/device-tree")) continue;
            if (normalized.startsWith(logical + "/") && (best == null || logical.length() > best.length())) best = logical;
        }
        return best == null ? null : paths.get(best) + normalized.substring(best.length());
    }
}
