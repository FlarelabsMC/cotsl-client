package com.flarelabsmc.cotsl.launch;

import com.sun.management.OperatingSystemMXBean;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class LaunchAgent {
    static final CountDownLatch LAUNCH_LATCH = new CountDownLatch(1);
    private static final String RELAUNCHED_PROP = "cotsl.relaunched";

    public static void premain(String agentArgs, Instrumentation inst) throws Exception {
        if (System.getProperty(RELAUNCHED_PROP) != null) return;
        LinuxQtState qtState = extendLibraryPathForQt();
        if (qtState == LinuxQtState.NO_QT) {
            System.err.println("[CotSL] Qt6 runtime not found, cannot launch launcher window. Crashing.");
            System.exit(1);
            return;
        }
        loadExtraJars(inst);
        try {
            LauncherWindow.create(LAUNCH_LATCH);
            if (LAUNCH_LATCH.getCount() > 0) System.exit(0);
        } catch (Throwable t) {
            System.err.println("[CotSL] Launcher unavailable (" + t.getMessage() + "), launching directly");
            return;
        }
        tryRelaunch();
    }

    private static void tryRelaunch() throws IOException {
        long totalRamMB = getTotalSystemRamMB();
        long recommended = totalRamMB > 0 ? computeMaxHeap(totalRamMB)
                : Runtime.getRuntime().maxMemory() / (1024 * 1024);
        try { doRelaunch(recommended); }
        catch (Exception e) { System.err.println("[CotSL] Could not relaunch with stock JVM args, continuing as is: " + e.getMessage()); }
    }

    private static void loadExtraJars(Instrumentation inst) throws Exception {
        File selfJar = findSelf();
        if (selfJar == null) {
            System.err.println("[CotSL] Could not locate own JAR, skipping extjarjar loading (this is fatal!)");
            return;
        }
        try (JarFile self = new JarFile(selfJar)) {
            List<JarEntry> entries = self.stream()
                    .filter(e -> e.getName().startsWith("META-INF/extjarjar/") && e.getName().endsWith(".jar"))
                    .toList();
            for (JarEntry entry : entries) {
                Path tmp = Files.createTempFile("cotsl-ext-", ".jar");
                tmp.toFile().deleteOnExit();
                try (InputStream in = self.getInputStream(entry)) {
                    Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                }
                inst.appendToSystemClassLoaderSearch(new JarFile(tmp.toFile()));
            }
        }
    }

    private static File findSelf() throws Exception {
        URI selfUri = LaunchAgent.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        File selfFile = new File(selfUri);
        if (selfFile.isFile()) return selfFile;
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (!arg.startsWith("-javaagent:")) continue;
            String path = arg.substring("-javaagent:".length());
            int eq = path.indexOf('=');
            if (eq >= 0) path = path.substring(0, eq);
            File f = new File(path);
            if (!f.isFile()) continue;
            try (JarFile jf = new JarFile(f)) {
                if (jf.getEntry("com/flarelabsmc/cotsl/launch/LaunchAgent.class") != null)
                    return f;
            } catch (Exception ignored) {}
        }
        return null;
    }

    enum LinuxQtState {
        HAS_QT,
        NO_QT,
        NO_LINUX
    }

    private static LinuxQtState extendLibraryPathForQt() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("linux")) return LinuxQtState.NO_LINUX;
        String[] candidates = {
                "/usr/lib",
                "/usr/lib/x86_64-linux-gnu",
                "/usr/lib64",
                "/usr/local/lib",
        };
        String current = System.getProperty("java.library.path", "");
        for (String dir : candidates) {
            if (new File(dir, "libQt6Core.so.6").exists()) {
                System.setProperty("java.library.path", current.isEmpty() ? dir : current + File.pathSeparator + dir);
                return LinuxQtState.HAS_QT;
            }
        }
        System.err.println("[CotSL] Qt6 runtime not found. This is fatal.");
        System.err.println("[CotSL] To fix this, install Qt6:");
        if (new File("/usr/bin/pacman").exists()) System.err.println("[CotSL] sudo pacman -S qt6-base qt6-declarative");
        else if (new File("/usr/bin/apt").exists()) System.err.println("[CotSL] sudo apt install libqt6core6t64 libqt6quick6 libqt6qml6 libqt6widgets6t64");
        else if (new File("/usr/bin/dnf").exists()) System.err.println("[CotSL] sudo dnf install qt6-qtbase qt6-qtdeclarative");
        else if (new File("/usr/bin/zypper").exists()) System.err.println("[CotSL] sudo zypper install libQt6Core6 libQt6Quick6 libQt6Qml6");
        else System.err.println("[CotSL] Install qt6-base and qt6-declarative via your package manager.");
        return LinuxQtState.NO_QT;
    }

    private static void doRelaunch(long maxHeapMB) throws Exception {
        String javaExecutable = ProcessHandle.current().info().command()
                .orElseGet(() -> System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        List<String> args = new ArrayList<>();
        boolean hasClassPath = false;
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (arg.startsWith("-Xmx") || arg.startsWith("-Xms")) continue;
            args.add(arg);
        }
        if (!hasClassPath) {
            String cp = System.getProperty("java.class.path");
            if (cp != null && !cp.isEmpty()) {
                args.add("-classpath");
                args.add(cp);
            }
        }
        args.add("-Xms512M");
        args.add("-Xmx" + maxHeapMB + "M");
        args.add("-D" + RELAUNCHED_PROP + "=true");
        File argFile = File.createTempFile("cotsl-jvmargs-", ".txt");
        argFile.deleteOnExit();
        try (PrintWriter pw = new PrintWriter(new FileWriter(argFile))) {
            for (String arg : args) pw.println(quoteForArgFile(arg));
        }
        List<String> programArgs = ProcessHandle.current().info().arguments()
                .map(LaunchAgent::extractProgramArgs)
                .filter(l -> !l.isEmpty())
                .orElseGet(() -> Arrays.asList(System.getProperty("sun.java.command", "").split(" ")));
        if (programArgs.isEmpty()) throw new IllegalStateException("Could not determine program arguments for relaunch");
        List<String> command = new ArrayList<>();
        command.add(javaExecutable);
        command.add("@" + argFile.getAbsolutePath());
        command.addAll(programArgs);
        int exitCode = new ProcessBuilder(command)
                .inheritIO()
                .start()
                .waitFor();
        System.exit(exitCode);
    }

    private static List<String> extractProgramArgs(String[] allArgs) {
        Set<String> jvmArgSet = new HashSet<>(ManagementFactory.getRuntimeMXBean().getInputArguments());
        for (int i = 0; i < allArgs.length; i++) {
            String a = allArgs[i];
            if (jvmArgSet.contains(a)) continue;
            if (a.startsWith("-X") || a.startsWith("-D") || a.startsWith("-ea") || a.startsWith("-da")) continue;
            if (a.startsWith("--add-") || a.startsWith("--enable-") || a.startsWith("--sun-")) continue;
            if (a.startsWith("@")) continue;
            return Arrays.asList(Arrays.copyOfRange(allArgs, i, allArgs.length));
        }
        return Collections.emptyList();
    }

    private static long getTotalSystemRamMB() {
        try {
            OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            return os.getTotalMemorySize() / (1024 * 1024);
        } catch (Exception e) { return -1; }
    }

    private static long computeMaxHeap(long totalRamMB) throws IOException {
        InputStream is = LaunchAgent.class.getResourceAsStream("/rec_mem_values.txt");
        if (is == null) throw new FileNotFoundException("rec_mem_values.txt not found in classpath");

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            List<long[]> entries = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length != 2) continue;
                entries.add(new long[]{
                        Long.parseLong(parts[0].trim()),
                        Long.parseLong(parts[1].trim())
                });
            }
            for (long[] entry : entries) {
                if (totalRamMB < entry[0]) return entry[1];
            }
            return entries.get(entries.size() - 1)[1];
        }
    }

    private static String quoteForArgFile(String arg) {
        String escaped = arg.replace("\\", "\\\\").replace("\"", "\\\"");
        if (arg.contains(" ") || arg.contains("\"")) return "\"" + escaped + "\"";
        return escaped;
    }
}
