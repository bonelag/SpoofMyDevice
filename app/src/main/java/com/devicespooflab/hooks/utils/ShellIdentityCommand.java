package com.devicespooflab.hooks.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Rewrite argv, never shell-parse or evaluate caller text. */
public final class ShellIdentityCommand {
    private ShellIdentityCommand() { }

    public static List<String> rewrite(List<String> command, Map<String, String> paths, String script) {
        if (command == null || command.isEmpty()) return null;
        String name = leaf(command.get(0));
        if (name.equals("su") || name.equals("sh") || name.equals("bash")) {
            int index = command.indexOf("-c");
            if (index < 0 || index + 1 >= command.size() || script == null || script.isEmpty()) return null;
            String prefix = ". " + ShellIdentityScript.quote(script) + "; ";
            if (command.get(index + 1).startsWith(prefix)) return null;
            List<String> result = new ArrayList<>(command);
            result.set(index + 1, prefix + command.get(index + 1));
            return result;
        }
        int firstArgument = 1;
        if ((name.equals("toybox") || name.equals("toolbox") || name.equals("busybox")) && command.size() > 1) {
            name = leaf(command.get(1));
            firstArgument = 2;
        }
        if (!name.equals("cat") && !name.equals("ls") && !name.equals("od") && !name.equals("head")
            && !name.equals("tail") && !name.equals("stat") && !name.equals("readlink")) return null;
        List<String> result = new ArrayList<>(command);
        boolean changed = false;
        for (int index = firstArgument; index < command.size(); index++) {
            String target = IdentityPaths.redirect(paths, command.get(index));
            if (target != null) { result.set(index, target); changed = true; }
        }
        return changed ? result : null;
    }

    private static String leaf(String name) {
        if (name == null) return "";
        int slash = name.lastIndexOf('/');
        return name.substring(slash + 1).toLowerCase(Locale.ROOT);
    }
}
