package com.devicespooflab.hooks.hooks;

import com.devicespooflab.hooks.utils.ConfigManager;
import com.devicespooflab.hooks.utils.GetPropOutputRewriter;
import com.devicespooflab.hooks.utils.RewritingProcess;
import com.devicespooflab.hooks.utils.InteractiveShellProcess;
import com.devicespooflab.hooks.utils.ScopedHardwareRuntime;
import com.devicespooflab.hooks.utils.ShellIdentityCommand;
import com.devicespooflab.hooks.utils.IdentityPaths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Hooks shell getprop access so apps cannot see original and spoofed values at the same time.
 * Many device info apps gather properties from both SystemProperties and a subprocess call to getprop.
 */
public final class GetPropHooks {

    private static final String TAG = "SpoofMyDevice-GetProp";

    private GetPropHooks() {
    }

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                ProcessBuilder.class,
                "start",
                new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        if (!ConfigManager.hasAssignedProfile()) return;
                        List<String> original = readCommand(param.thisObject);
                        List<String> wrapped = scopedShellCommand(original);
                        if (wrapped != null) {
                            param.setObjectExtra("smd.originalCommand", original);
                            ((ProcessBuilder) param.thisObject).command(wrapped);
                        }
                    }
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object saved = param.getObjectExtra("smd.originalCommand");
                        if (saved instanceof List) ((ProcessBuilder) param.thisObject).command((List<String>) saved);
                        Process originalProcess = (Process) param.getResult();
                        if (originalProcess == null || originalProcess instanceof RewritingProcess
                            || originalProcess instanceof InteractiveShellProcess
                            || !ConfigManager.hasAssignedProfile()) {
                            return;
                        }

                        List<String> command = readCommand(param.thisObject);
                        if (isInteractiveShell(command)) {
                            param.setResult(new InteractiveShellProcess(originalProcess, () -> ScopedHardwareRuntime.current().shellScript));
                            return;
                        }
                        CommandRequest request = parseRequest(command);
                        if (request == null) {
                            return;
                        }

                        Process replacement = buildReplacementProcess(originalProcess, request, lpparam.packageName);
                        if (replacement != null) {
                            param.setResult(replacement);
                        }
                    }
                }
            );
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + ": Failed to hook ProcessBuilder.start(): " + throwable.getMessage());
        }

        hookRuntimeExec(lpparam.packageName);
    }

    private static void hookRuntimeExec(String packageName) {
        hookRuntimeExecVariant(packageName, new Object[]{String.class});
        hookRuntimeExecVariant(packageName, new Object[]{String[].class});
        hookRuntimeExecVariant(packageName, new Object[]{String.class, String[].class});
        hookRuntimeExecVariant(packageName, new Object[]{String[].class, String[].class});
        hookRuntimeExecVariant(packageName, new Object[]{String.class, String[].class, java.io.File.class});
        hookRuntimeExecVariant(packageName, new Object[]{String[].class, String[].class, java.io.File.class});
    }

    private static void hookRuntimeExecVariant(String packageName, Object[] parameterTypes) {
        Object[] hookArgs = Arrays.copyOf(parameterTypes, parameterTypes.length + 1);
        hookArgs[parameterTypes.length] = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                if (!ConfigManager.hasAssignedProfile() || !(param.args[0] instanceof String[])) return;
                String[] original = (String[]) param.args[0];
                List<String> wrapped = scopedShellCommand(Arrays.asList(original));
                if (wrapped != null) {
                    param.setObjectExtra("smd.originalCommand", original);
                    param.args[0] = wrapped.toArray(new String[0]);
                }
            }
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Process originalProcess = (Process) param.getResult();
                if (originalProcess == null || originalProcess instanceof RewritingProcess
                    || originalProcess instanceof InteractiveShellProcess
                    || !ConfigManager.hasAssignedProfile()
                    || param.args.length == 0 || param.args[0] == null) {
                    return;
                }

                CommandRequest request = null;
                Object saved = param.getObjectExtra("smd.originalCommand");
                Object commandArg = saved == null ? param.args[0] : saved;
                List<String> command = commandArg instanceof String[] ? Arrays.asList((String[]) commandArg)
                    : commandArg instanceof String ? Arrays.asList(((String) commandArg).trim().split("\\s+")) : null;
                if (isInteractiveShell(command)) {
                    param.setResult(new InteractiveShellProcess(originalProcess, () -> ScopedHardwareRuntime.current().shellScript));
                    return;
                }
                if (commandArg instanceof String) {
                    request = parseShellCommand((String) commandArg);
                } else if (commandArg instanceof String[]) {
                    request = parseRequest(Arrays.asList((String[]) commandArg));
                }

                if (request == null) {
                    return;
                }

                Process replacement = buildReplacementProcess(originalProcess, request, packageName);
                if (replacement != null) {
                    param.setResult(replacement);
                }
            }
        };

        try {
            XposedHelpers.findAndHookMethod(Runtime.class, "exec", hookArgs);
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> readCommand(Object processBuilder) {
        try {
            Object value = XposedHelpers.callMethod(processBuilder, "command");
            if (value instanceof List) {
                return new ArrayList<>((List<String>) value);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean isInteractiveShell(List<String> command) {
        if (command == null || command.isEmpty() || command.contains("-c")) return false;
        String name = leafName(command.get(0));
        return name.equals("su") || name.equals("sh") || name.equals("bash");
    }

    private static List<String> scopedShellCommand(List<String> command) {
        ScopedHardwareRuntime.View view = ScopedHardwareRuntime.current();
        return ShellIdentityCommand.rewrite(command, view.paths, view.shellScript);
    }

    private static CommandRequest parseRequest(List<String> command) {
        if (command == null || command.isEmpty()) {
            return null;
        }

        List<String> normalized = new ArrayList<>(command.size());
        for (String part : command) {
            normalized.add(part == null ? "" : part.trim());
        }

        String first = leafName(normalized.get(0));
        if ((first.equals("toybox") || first.equals("toolbox") || first.equals("busybox")) && normalized.size() > 1) {
            normalized = new ArrayList<>(normalized.subList(1, normalized.size()));
            first = leafName(normalized.get(0));
        }
        if ("getprop".equals(first)) {
            if (normalized.size() > 3 || (normalized.size() >= 2 && normalized.get(1).startsWith("-"))) {
                return null;
            }
            if (normalized.size() >= 2) {
                return new CommandRequest(false, normalized.get(1));
            }
            return new CommandRequest(true, null);
        }

        if ("uname".equals(first)) {
            String output = ConfigManager.hardwareIdentity().unameOutput(normalized.subList(1, normalized.size()));
            return output == null ? null : new CommandRequest(output);
        }

        if ("cat".equals(first) && normalized.size() == 2) {
            String path = normalized.get(1);
            CommandRequest catReq = handleCatCommand(path);
            if (catReq != null) return catReq;
        }
        if ("ls".equals(first) && normalized.size() == 2) {
            List<String> entries = ConfigManager.hardwareIdentity().directories().get(IdentityPaths.normalize(normalized.get(1)));
            if (entries != null) return new CommandRequest(entries.isEmpty() ? "" : String.join("\n", entries) + "\n");
        }

        if (("sh".equals(first) || "su".equals(first)) && normalized.size() >= 3) {
            int commandIndex = normalized.indexOf("-c");
            if (commandIndex >= 0 && commandIndex + 1 < normalized.size()) {
                return parseShellCommand(normalized.get(commandIndex + 1));
            }
        }
        return null;
    }

    private static CommandRequest parseShellCommand(String shellCommand) {
        if (shellCommand == null) {
            return null;
        }
        String trimmed = shellCommand.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        // Compound commands can run arbitrary children; only rewrite a simple command.
        if (trimmed.matches("(?s).*[|&;<>\\r\\n`$'\"\\\\].*")) {
            return null;
        }
        String[] parts = trimmed.split("\\s+");
        if (parts.length == 0) {
            return null;
        }
        return parseRequest(Arrays.asList(parts));
    }

    private static CommandRequest handleCatCommand(String path) {
        if (path == null) return null;
        // All transports use the same per-core content and toggles. No older generic CPU fallback.
        String content = ConfigManager.hardwareIdentity().files().get(IdentityPaths.normalize(path));
        return content == null ? null : new CommandRequest(content);
    }

    private static Process buildReplacementProcess(Process originalProcess, CommandRequest request, String packageName) {
        if (!request.isGetProp) {
            return RewritingProcess.replaceOutput(originalProcess,
                request.staticOutput != null ? request.staticOutput : "\n");
        }

        if (request.fullDump) {
            Map<String, String> spoofed = new LinkedHashMap<>(ConfigManager.getEffectiveSystemProperties());
            if (ConfigManager.shouldBypassVersionSpoof(packageName)) {
                spoofed.entrySet().removeIf(entry -> isVersionProperty(entry.getKey()));
            }
            return new RewritingProcess(originalProcess,
                new GetPropOutputRewriter(spoofed, ConfigManager::isHostLeakProperty));
        }

        String key = request.propertyKey;
        if (key == null || key.isEmpty()) {
            return null;
        }
        if (ConfigManager.shouldBypassVersionSpoof(packageName) && isVersionProperty(key)) {
            return null;
        }

        String spoofedValue = ConfigManager.getSystemProperty(key, null);
        if (spoofedValue == null && ConfigManager.isHostLeakProperty(key)) {
            spoofedValue = "";
        }
        if (spoofedValue == null) {
            return null;
        }

        String replacementOutput = spoofedValue + "\n";
        return RewritingProcess.replaceOutput(originalProcess, replacementOutput);
    }

    private static String leafName(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String normalized = value.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        return (slash >= 0 ? normalized.substring(slash + 1) : normalized).toLowerCase(Locale.US);
    }

    private static boolean isVersionProperty(String key) {
        return key != null
            && (key.startsWith("ro.build.version.")
                || key.startsWith("ro.product.build.version.")
                || key.contains(".build.version."));
    }

    private static final class CommandRequest {
        final boolean isGetProp;
        final boolean fullDump;
        final String propertyKey;
        final String staticOutput;

        CommandRequest(boolean fullDump, String propertyKey) {
            this.isGetProp = true;
            this.fullDump = fullDump;
            this.propertyKey = propertyKey;
            this.staticOutput = null;
        }

        CommandRequest(String staticOutput) {
            this.isGetProp = false;
            this.fullDump = false;
            this.propertyKey = null;
            this.staticOutput = staticOutput;
        }
    }

}
