package com.devicespooflab.hooks.utils;

import java.util.LinkedHashMap;
import java.util.Map;

/** A reported SDK must not enable methods/fields that the installed OS does not provide. */
public final class RuntimeApiLevelPolicy {
    private RuntimeApiLevelPolicy() { }

    /** Call with the effective SDK, after limit(); equal SDKs need no runtime hooks. */
    public static boolean needsRuntimeSdkScope(int reportedSdk, int actualSdk) {
        return reportedSdk > 0 && reportedSdk != actualSdk;
    }

    public static Map<String, String> limit(Map<String, String> configured, int actualSdk) {
        Map<String, String> result = new LinkedHashMap<>(configured);
        for (Map.Entry<String, String> entry : result.entrySet()) {
            if (!entry.getKey().contains(".build.version.") || !entry.getKey().endsWith(".sdk")) continue;
            try {
                int requested = Integer.parseInt(entry.getValue());
                if (requested > actualSdk || requested <= 0) entry.setValue(String.valueOf(actualSdk));
            } catch (NumberFormatException invalid) { entry.setValue(String.valueOf(actualSdk)); }
        }
        return result;
    }
}
