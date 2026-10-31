package com.devicespooflab.hooks.hooks;

import com.devicespooflab.hooks.utils.ConfigManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Process originalProcess = (Process) param.getResult();
                        if (originalProcess == null) {
                            return;
                        }

                        List<String> command = readCommand(param.thisObject);
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
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Process originalProcess = (Process) param.getResult();
                if (originalProcess == null || param.args.length == 0 || param.args[0] == null) {
                    return;
                }

                CommandRequest request = null;
                Object commandArg = param.args[0];
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

    private static CommandRequest parseRequest(List<String> command) {
        if (command == null || command.isEmpty()) {
            return null;
        }

        List<String> normalized = new ArrayList<>(command.size());
        for (String part : command) {
            normalized.add(part == null ? "" : part.trim());
        }

        String first = leafName(normalized.get(0));
        if ("getprop".equals(first)) {
            if (normalized.size() >= 2) {
                return new CommandRequest(false, normalized.get(1));
            }
            return new CommandRequest(true, null);
        }

        if ("uname".equals(first)) {
            boolean hasM = false;
            boolean hasA = false;
            for (String arg : normalized) {
                if (arg.contains("-m")) hasM = true;
                if (arg.contains("-a")) hasA = true;
            }
            String abi = ConfigManager.getCpuAbi();
            String arch = (abi != null && abi.contains("arm64")) ? "aarch64" : "armv7l";
            if (hasM) {
                return new CommandRequest(arch + "\n");
            }
            if (hasA) {
                return new CommandRequest("Linux localhost 5.15.0 #1 SMP PREEMPT " + arch + "\n");
            }
            return new CommandRequest("Linux\n");
        }

        if ("cat".equals(first) && normalized.size() >= 2) {
            String path = normalized.get(1);
            CommandRequest catReq = handleCatCommand(path);
            if (catReq != null) return catReq;
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
        String[] parts = trimmed.split("\\s+");
        if (parts.length == 0) {
            return null;
        }
        String first = leafName(parts[0]);
        if ("getprop".equals(first)) {
            if (parts.length >= 2) {
                return new CommandRequest(false, parts[1]);
            }
            return new CommandRequest(true, null);
        }

        if ("uname".equals(first)) {
            boolean hasM = false;
            boolean hasA = false;
            for (String part : parts) {
                if (part.contains("-m")) hasM = true;
                if (part.contains("-a")) hasA = true;
            }
            String abi = ConfigManager.getCpuAbi();
            String arch = (abi != null && abi.contains("arm64")) ? "aarch64" : "armv7l";
            if (hasM) {
                return new CommandRequest(arch + "\n");
            }
            if (hasA) {
                return new CommandRequest("Linux localhost 5.15.0 #1 SMP PREEMPT " + arch + "\n");
            }
            return new CommandRequest("Linux\n");
        }

        if ("cat".equals(first) && parts.length >= 2) {
            String path = parts[1];
            CommandRequest catReq = handleCatCommand(path);
            if (catReq != null) return catReq;
        }
        return null;
    }

    private static CommandRequest handleCatCommand(String path) {
        if (path == null) return null;
        if (path.equals("/proc/cpuinfo")) {
            return new CommandRequest(ConfigManager.getCpuInfoContent());
        }
        if (path.contains("present") || path.contains("online") || path.contains("possible")) {
            return new CommandRequest("0-" + (ConfigManager.getCpuCores() - 1) + "\n");
        }
        if (path.contains("/cache/index3/size")) {
            return new CommandRequest(ConfigManager.getCacheL3Size() + "\n");
        }
        if (path.contains("max_freq")) {
            return new CommandRequest(ConfigManager.getCpuMaxFreq() + "\n");
        }
        if (path.contains("min_freq")) {
            return new CommandRequest(ConfigManager.getCpuMinFreq() + "\n");
        }
        return null;
    }

    private static Process buildReplacementProcess(Process originalProcess, CommandRequest request, String packageName) {
        if (!request.isGetProp) {
            readStream(originalProcess.getInputStream());
            readStream(originalProcess.getErrorStream());
            waitForExit(originalProcess);
            return new SimpleProcess(request.staticOutput != null ? request.staticOutput : "\n", "", 0);
        }

        if (request.fullDump) {
            String originalStdout = readStream(originalProcess.getInputStream());
            String originalStderr = readStream(originalProcess.getErrorStream());
            int exitCode = waitForExit(originalProcess);
            String rewritten = rewriteGetPropDump(originalStdout, packageName);
            return new SimpleProcess(rewritten, originalStderr, exitCode);
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

        readStream(originalProcess.getInputStream());
        String originalStderr = readStream(originalProcess.getErrorStream());
        int exitCode = waitForExit(originalProcess);
        return new SimpleProcess(spoofedValue + "\n", originalStderr, exitCode);
    }

    private static String rewriteGetPropDump(String stdout, String packageName) {
        Map<String, String> spoofed = new LinkedHashMap<>(ConfigManager.getEffectiveSystemProperties());
        if (ConfigManager.shouldBypassVersionSpoof(packageName)) {
            spoofed.entrySet().removeIf(entry -> isVersionProperty(entry.getKey()));
        }
        if (spoofed.isEmpty()) {
            return stdout;
        }

        Map<String, String> merged = new LinkedHashMap<>();
        if (stdout != null && !stdout.isEmpty()) {
            List<String> lines = Arrays.asList(stdout.split("\\r?\\n"));
            for (String line : lines) {
                ParsedProperty parsed = parsePropertyLine(line);
                if (parsed == null) {
                    continue;
                }
                merged.put(parsed.key, parsed.value);
            }
        }

        merged.entrySet().removeIf(entry -> ConfigManager.isHostLeakProperty(entry.getKey()));
        merged.putAll(spoofed);

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : merged.entrySet()) {
            builder.append('[')
                .append(entry.getKey())
                .append("]: [")
                .append(entry.getValue() == null ? "" : entry.getValue())
                .append("]\n");
        }
        return builder.toString();
    }

    private static ParsedProperty parsePropertyLine(String line) {
        if (line == null) {
            return null;
        }
        String trimmed = line.trim();
        if (!trimmed.startsWith("[") || !trimmed.contains("]: [") || !trimmed.endsWith("]")) {
            return null;
        }
        int middle = trimmed.indexOf("]: [");
        if (middle <= 1) {
            return null;
        }
        String key = trimmed.substring(1, middle);
        String value = trimmed.substring(middle + 4, trimmed.length() - 1);
        return new ParsedProperty(key, value);
    }

    private static String readStream(InputStream inputStream) {
        if (inputStream == null) {
            return "";
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        } catch (IOException ignored) {
            return "";
        }
    }

    private static int waitForExit(Process process) {
        try {
            return process.waitFor();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            return 0;
        }
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

    private static final class ParsedProperty {
        final String key;
        final String value;

        ParsedProperty(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    private static final class SimpleProcess extends Process {
        private final ByteArrayInputStream inputStream;
        private final ByteArrayInputStream errorStream;
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        private final int exitCode;

        SimpleProcess(String stdout, String stderr, int exitCode) {
            this.inputStream = new ByteArrayInputStream((stdout == null ? "" : stdout).getBytes(StandardCharsets.UTF_8));
            this.errorStream = new ByteArrayInputStream((stderr == null ? "" : stderr).getBytes(StandardCharsets.UTF_8));
            this.exitCode = exitCode;
        }

        @Override
        public OutputStream getOutputStream() {
            return outputStream;
        }

        @Override
        public InputStream getInputStream() {
            return inputStream;
        }

        @Override
        public InputStream getErrorStream() {
            return errorStream;
        }

        @Override
        public int waitFor() {
            return exitCode;
        }

        @Override
        public int exitValue() {
            return exitCode;
        }

        @Override
        public void destroy() {
        }
    }
}
