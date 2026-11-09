package com.devicespooflab.hooks.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** POSIX identity paths must have identical semantics on Android, in shells and in JVM tests. */
public final class IdentityPaths {
    private IdentityPaths() { }

    public static String normalize(String path) {
        if (path == null || !path.startsWith("/") || path.indexOf('\0') >= 0) return null;
        if (isNormalized(path)) return path;
        Deque<String> parts = new ArrayDeque<>();
        int start = 1;
        while (start <= path.length()) {
            int slash = path.indexOf('/', start);
            int end = slash < 0 ? path.length() : slash;
            String part = path.substring(start, end);
            if (part.equals("..")) {
                if (!parts.isEmpty()) parts.removeLast();
            } else if (!part.isEmpty() && !part.equals(".")) parts.addLast(part);
            if (slash < 0) break;
            start = slash + 1;
        }
        return "/" + String.join("/", parts);
    }

    private static boolean isNormalized(String path) {
        if (path.length() == 1) return true;
        if (path.endsWith("/")) return false;
        for (int index = 1; index < path.length(); index++) {
            if (path.charAt(index - 1) != '/') continue;
            char current = path.charAt(index);
            if (current == '/') return false;
            if (current == '.' && (index + 1 == path.length() || path.charAt(index + 1) == '/'
                || (path.charAt(index + 1) == '.' && (index + 2 == path.length() || path.charAt(index + 2) == '/')))) return false;
        }
        return true;
    }

    public static String redirect(Map<String, String> paths, String path) {
        if (paths.isEmpty()) return null;
        String normalized = normalize(path);
        if (normalized == null) return null;
        return redirectNormalized(paths, normalized);
    }

    private static String redirectNormalized(Map<String, String> paths, String normalized) {
        String exact = paths.get(normalized);
        if (exact != null) return exact;
        int slash = normalized.lastIndexOf('/');
        while (slash > 0) {
            String logical = normalized.substring(0, slash);
            // A proc file is not a directory. Directory inventories in procfs are explicit roots.
            if (!logical.startsWith("/proc/") || logical.equals("/proc/device-tree") || logical.startsWith("/proc/device-tree/")) {
                String target = paths.get(logical);
                if (target != null) return target + normalized.substring(slash);
            }
            slash = normalized.lastIndexOf('/', slash - 1);
        }
        return null;
    }

    /** Keep exceptions, but omit children already owned by the same virtual inventory. */
    public static Map<String, String> compact(Map<String, String> paths) {
        List<Map.Entry<String, String>> entries = new ArrayList<>(paths.entrySet());
        entries.sort(Comparator.comparingInt(entry -> entry.getKey().length()));
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : entries) {
            if (!entry.getValue().equals(redirect(result, entry.getKey()))) result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /** Prepared once on the worker; unrelated app/cache paths allocate no lookup strings. */
    public static final class Index {
        private final Map<String, String> paths;
        private final String[] roots;

        public Index(Map<String, String> paths) {
            this.paths = Collections.unmodifiableMap(new LinkedHashMap<>(compact(paths)));
            Set<String> prefixes = new LinkedHashSet<>();
            for (String path : this.paths.keySet()) {
                int slash = path.indexOf('/', 1);
                prefixes.add(slash < 0 ? path : path.substring(0, slash));
            }
            roots = prefixes.toArray(new String[0]);
        }

        public Map<String, String> paths() { return paths; }

        public String redirect(String path) {
            if (paths.isEmpty()) return null;
            String normalized = normalize(path);
            if (normalized == null) return null;
            for (String root : roots) {
                if (normalized.startsWith(root) && (normalized.length() == root.length()
                    || normalized.charAt(root.length()) == '/')) return redirectNormalized(paths, normalized);
            }
            return null;
        }
    }
}
