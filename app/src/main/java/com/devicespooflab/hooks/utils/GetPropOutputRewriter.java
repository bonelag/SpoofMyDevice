package com.devicespooflab.hooks.utils;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Transforms a getprop dump without waiting for its final line or losing unrelated stdout. */
public final class GetPropOutputRewriter implements RewritingProcess.OutputRewriter {
    private final Map<String, String> properties;
    private final Predicate<String> dropProperty;
    private final Set<String> seen = new HashSet<>();
    private boolean needsNewline;

    public GetPropOutputRewriter(Map<String, String> properties, Predicate<String> dropProperty) {
        this.properties = new LinkedHashMap<>(properties);
        this.properties.keySet().removeIf(HookSafetyPolicy::isRuntimeStateProperty);
        this.dropProperty = dropProperty;
    }

    @Override
    public String rewriteLine(String line) {
        String trimmed = line.trim();
        int middle = trimmed.indexOf("]: [");
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]") || middle <= 1) {
            recordEnding(line);
            return line;
        }
        String key = trimmed.substring(1, middle);
        seen.add(key);
        if (HookSafetyPolicy.isRuntimeStateProperty(key)) {
            recordEnding(line);
            return line;
        }
        if (dropProperty.test(key)) {
            return "";
        }
        if (!properties.containsKey(key)) {
            recordEnding(line);
            return line;
        }
        String ending = line.endsWith("\r\n") ? "\r\n" : line.endsWith("\n") ? "\n" : "";
        String output = formatProperty(key, properties.get(key)) + ending;
        recordEnding(output);
        return output;
    }

    @Override
    public String finish() {
        StringBuilder tail = new StringBuilder();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (!seen.contains(entry.getKey()) && !dropProperty.test(entry.getKey())) {
                if (tail.length() == 0 && needsNewline) {
                    tail.append('\n');
                }
                tail.append(formatProperty(entry.getKey(), entry.getValue())).append('\n');
            }
        }
        return tail.toString();
    }

    private void recordEnding(String output) {
        if (!output.isEmpty()) {
            needsNewline = !output.endsWith("\n");
        }
    }

    private static String formatProperty(String key, String value) {
        return "[" + key + "]: [" + (value == null ? "" : value) + "]";
    }
}
