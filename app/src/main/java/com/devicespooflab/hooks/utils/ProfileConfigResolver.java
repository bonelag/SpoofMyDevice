package com.devicespooflab.hooks.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/** A null result means unavailable data; an empty snapshot means an explicit unassignment. */
public final class ProfileConfigResolver {
    private ProfileConfigResolver() {
    }

    public static RuntimeConfigStore.Snapshot resolvePreferences(
        Map<String, ?> preferences, String packageName, boolean allowDefault
    ) {
        Object json = preferences.get("json_state");
        if (json instanceof String) {
            // The complete state also records removals. Never fall back to a stale assignment.
            return resolveJson((String) json, packageName, allowDefault);
        }
        Object assigned = preferences.get("pkg_" + packageName);
        if (assigned instanceof String) {
            return resolveContent((String) assigned);
        }
        Object defaultConfig = preferences.get("default_config");
        if (allowDefault && defaultConfig instanceof String) {
            return resolveContent((String) defaultConfig);
        }
        return null;
    }

    public static RuntimeConfigStore.Snapshot resolveJson(
        String json, String packageName, boolean allowDefault
    ) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            JSONObject root = new JSONObject(json);
            if (root.getInt("version") != 1) {
                return null;
            }
            JSONObject assignments = root.getJSONObject("assignments");
            JSONObject globalProperties = root.getJSONObject("globalProperties");
            JSONArray profiles = root.getJSONArray("profiles");
            if (profiles.length() == 0) {
                return null;
            }

            String profileId = assignments.optString(packageName, "");
            if (profileId.isEmpty() && allowDefault) {
                profileId = globalProperties.optString(ConfigManager.KEY_DEFAULT_PROFILE_ID, "");
            }
            String content = null;
            for (int index = 0; index < profiles.length(); index++) {
                JSONObject profile = profiles.getJSONObject(index);
                String id = profile.getString("id");
                String profileContent = profile.getString("content");
                if (!profileId.isEmpty() && profileId.equals(id)) {
                    content = profileContent;
                }
            }
            if (content == null) {
                return resolveContent("");
            }

            Map<String, String> properties = parseProperties(content);
            java.util.Iterator<String> keys = globalProperties.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (!HookSafetyPolicy.isRuntimeStateProperty(key)) {
                    properties.put(key, globalProperties.getString(key));
                }
            }
            return new RuntimeConfigStore.Snapshot(properties, false);
        } catch (Exception ignored) {
            // A partial write or unsupported state must not remove the last working profile.
            return null;
        }
    }

    public static RuntimeConfigStore.Snapshot resolveContent(String content) {
        return content == null ? null : new RuntimeConfigStore.Snapshot(parseProperties(content), false);
    }

    private static Map<String, String> parseProperties(String content) {
        Map<String, String> properties = new LinkedHashMap<>();
        for (String rawLine : content.split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int equals = line.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            String key = line.substring(0, equals).trim();
            String value = line.substring(equals + 1).trim();
            if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            if (!HookSafetyPolicy.isRuntimeStateProperty(key)) {
                properties.put(key, value);
            }
        }
        return properties;
    }
}
