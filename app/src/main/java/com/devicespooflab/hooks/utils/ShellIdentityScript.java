package com.devicespooflab.hooks.utils;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** A scoped shell sources this generated file. Profile values are always literal arguments. */
public final class ShellIdentityScript {
    public static final String RESET = "unset -f getprop cat ls uname cd pwd od head tail stat readlink __smd_path __smd_command\n";
    private ShellIdentityScript() { }
    public static String quote(String value) { return "'" + value.replace("'", "'\\''") + "'"; }

    public static String build(Map<String, String> input, Map<String, String> paths) {
        Map<String, String> properties = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : input.entrySet()) {
            String key = entry.getKey();
            if (!key.matches("[A-Za-z0-9_.-]+") || HookSafetyPolicy.isRuntimeStateProperty(key)) continue;
            if (!(key.startsWith("ro.") || key.startsWith("persist.") || key.startsWith("gsm.")
                || key.startsWith("vendor.") || key.startsWith("dalvik.") || key.startsWith("sys."))) continue;
            properties.put(key, entry.getValue() == null ? "" : entry.getValue());
        }
        StringBuilder script = new StringBuilder(RESET);
        script.append("getprop() {\nif [ \"$#\" -eq 0 ]; then\ncommand /system/bin/getprop | {\nwhile IFS= read -r __smd_line; do\ncase \"$__smd_line\" in\n");
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            script.append(quote("[" + entry.getKey() + "]: [")).append("*) :;;\n");
        }
        boolean nonVivo = !input.getOrDefault("ro.product.brand", "").matches("(?i).*(vivo|iqoo).*");
        String soc = input.getOrDefault("ro.soc.manufacturer", "") + " " + input.getOrDefault("ro.soc.model", "");
        boolean nonMediatek = !soc.trim().isEmpty() && !soc.toLowerCase(java.util.Locale.ROOT).matches(".*(mediatek|mtk).*")
            && !"false".equalsIgnoreCase(input.get("spoof.enabled.soc_model")) && !"0".equals(input.get("spoof.enabled.soc_model"));
        if (nonVivo) script.append("'[ro.vivo.'*|'[ro.vendor.vivo.'*|'[persist.vivo.'*|'[vendor.gsm.project.'*) :;;\n");
        if (nonMediatek) script.append("'[ro.mediatek.'*|'[ro.vendor.mediatek.'*|'[ro.vendor.mtk_'*|'[ro.mtk.'*) :;;\n");
        script.append("*) printf '%s\\n' \"$__smd_line\";;\nesac\ndone\n}\n");
        if (!properties.isEmpty()) {
            script.append("printf '%s\\n'");
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                script.append(" ").append(quote("[" + entry.getKey() + "]: [" + entry.getValue() + "]"));
            }
            script.append('\n');
        }
        script.append("else\ncase \"$1\" in\n");
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            script.append(quote(entry.getKey())).append(") printf '%s\\n' ").append(quote(entry.getValue())).append(";;\n");
        }
        if (nonVivo) script.append("ro.vivo.*|ro.vendor.vivo.*|persist.vivo.*|vendor.gsm.project.*) printf '\\n';;\n");
        if (nonMediatek) script.append("ro.mediatek.*|ro.vendor.mediatek.*|ro.vendor.mtk_*|ro.mtk.*) printf '\\n';;\n");
        script.append("*) command /system/bin/getprop \"$@\";;\nesac\nfi\n}\n");
        List<Map.Entry<String, String>> entries = new ArrayList<>(IdentityPaths.compact(paths).entrySet());
        entries.sort(Comparator.comparingInt((Map.Entry<String, String> entry) -> entry.getKey().length()).reversed());
        script.append("__smd_path() {\n__smd_path_result=\"$1\"\ncase \"$1\" in -*|'') return;; /*) __smd_input=\"$1\";; *) __smd_input=\"$PWD/$1\";; esac\n")
            .append("__smd_saved_ifs=\"$IFS\"\n__smd_saved_f=0\ncase \"$-\" in *f*) __smd_saved_f=1;; esac\n")
            .append("set -f\nIFS=/\nset -- $__smd_input\nIFS=\"$__smd_saved_ifs\"\n[ \"$__smd_saved_f\" -eq 1 ] || set +f\n")
            .append("__smd_normalized=''\nfor __smd_part do\ncase \"$__smd_part\" in ''|.) :;; ..) __smd_normalized=\"${__smd_normalized%/*}\";; *) __smd_normalized=\"$__smd_normalized/$__smd_part\";; esac\ndone\n")
            .append("__smd_normalized=\"${__smd_normalized:-/}\"\ncase \"$__smd_normalized\" in\n");
        List<Map.Entry<String, String>> targets = new ArrayList<>(entries);
        targets.sort(Comparator.comparingInt((Map.Entry<String, String> entry) -> entry.getValue().length()).reversed());
        appendPathCases(script, targets, true);
        script.append("esac\n__smd_logical=\"$__smd_normalized\"\n__smd_path_result=\"$__smd_normalized\"\ncase \"$__smd_normalized\" in\n");
        appendPathCases(script, entries, false);
        script.append("esac\n}\n__smd_command() {\n__smd_program=\"$1\"\nshift\nfor __smd_original do\n")
            .append("__smd_path \"$__smd_original\"\nshift\nset -- \"$@\" \"$__smd_path_result\"\ndone\n")
            .append("command \"$__smd_program\" \"$@\"\n}\n");
        for (String command : new String[]{"cat", "ls", "od", "head", "tail", "stat", "readlink"}) {
            script.append(command).append("() { __smd_command /system/bin/").append(command).append(" \"$@\"; }\n");
        }
        script.append("cd() { __smd_command cd \"$@\"; }\npwd() {\ncase \"$*\" in ''|-L|-P) __smd_path .; printf '%s\\n' \"$__smd_logical\";; *) command pwd \"$@\";; esac\n}\n");
        if ("true".equals(input.get("identity.kernel.enabled"))) {
            String release = input.getOrDefault("identity.kernel.release", "");
            String version = input.getOrDefault("identity.kernel.version", "");
            String machine = input.getOrDefault("identity.kernel.machine", "aarch64");
            String hostname = input.getOrDefault("identity.kernel.hostname", "localhost");
            script.append("uname() {\n__smd_flags=''\n[ \"$#\" -gt 0 ] || __smd_flags=s\nfor __smd_arg do\n")
                .append("case \"$__smd_arg\" in -?*) __smd_arg=\"${__smd_arg#-}\";; *) command /system/bin/uname \"$@\"; return $?;; esac\n")
                .append("while [ -n \"$__smd_arg\" ]; do\n__smd_rest=\"${__smd_arg#?}\"\n__smd_flag=\"${__smd_arg%\"$__smd_rest\"}\"\n__smd_arg=\"$__smd_rest\"\n")
                .append("case \"$__smd_flag\" in a) __smd_flags=\"${__smd_flags}snrvmo\";; s|n|r|v|m|p|i|o) __smd_flags=\"$__smd_flags$__smd_flag\";; *) command /system/bin/uname \"$@\"; return $?;; esac\ndone\ndone\n__smd_uname_out=''\n");
            String[] values = {"Linux", hostname, release, version, machine, machine, machine, "Android"};
            String flags = "snrvmpio";
            for (int index = 0; index < flags.length(); index++) {
                script.append("case \"$__smd_flags\" in *").append(flags.charAt(index)).append("*) __smd_uname_out=\"${__smd_uname_out}${__smd_uname_out:+ }\"")
                    .append(quote(values[index])).append(";; esac\n");
            }
            script.append("printf '%s\\n' \"$__smd_uname_out\"\n}\n");
        }
        return script.toString();
    }

    private static void appendPathCases(StringBuilder script, List<Map.Entry<String, String>> entries, boolean reverse) {
        for (Map.Entry<String, String> entry : entries) {
            String source = reverse ? entry.getValue() : entry.getKey();
            String target = reverse ? entry.getKey() : entry.getValue();
            String variable = reverse ? "__smd_normalized" : "__smd_path_result";
            script.append(quote(source)).append(") ").append(variable).append('=').append(quote(target)).append(";;\n");
            if (!entry.getKey().startsWith("/proc/") || entry.getKey().startsWith("/proc/device-tree")) {
                script.append(quote(source + "/")).append("*) ").append(variable).append('=').append(quote(target))
                    .append("\"${__smd_normalized#").append(quote(source)).append("}\";;\n");
            }
        }
    }
}
