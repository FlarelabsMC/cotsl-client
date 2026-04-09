package com.flarelabsmc.cotsl.launch;

import com.sun.management.OperatingSystemMXBean;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * the agent itself, used to launch the launcher.
 * TODO in the future this JAR should be runnable on its own and hook into the NeoForge installer to grab Minecraft's install location
 * TODO contributors, feel free to figure this one out while I work on the launcher UI
 */
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
        File selfJar = findSelf();
        loadExtraJars(inst, selfJar);
        extractQtNatives(selfJar);
        try {
            LauncherWindow.create(LAUNCH_LATCH);
            if (LAUNCH_LATCH.getCount() > 0) System.exit(0);
        } catch (Throwable t) {
            System.err.println("[CotSL] Launcher unavailable (" + t.getMessage() + "), launching directly");
            return;
        }
        tryRelaunch();
    }

    /**
     * after the agent overrides launch, it grabs the arguments from when it tried to launch as Minecraft, and relaunches the game with proper JVM arguments
     * @throws IOException
     */
    private static void tryRelaunch() throws IOException {
        long totalRamMB = getTotalSystemRamMB();
        long recommended = totalRamMB > 0 ? computeMaxHeap(totalRamMB) : Runtime.getRuntime().maxMemory() / (1024 * 1024);
        try { doRelaunch(recommended); }
        catch (Exception e) { System.err.println("[CotSL] Could not relaunch with stock JVM args, continuing as is: " + e.getMessage()); }
    }

    /**
     * loads the JARs within the META-INF/extjarjar folder, used by the agent because NeoForge's JarJar does not apply at this stage
     * @param inst
     * @param selfJar
     * @throws Exception
     */
    private static void loadExtraJars(Instrumentation inst, File selfJar) throws Exception {
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

    /**
     * extracts qt natives for windows and mac to the default temp folder. tries to reuse them, else deletes them
     * @param selfJar the mod jar itself
     */
    private static void extractQtNatives(File selfJar) {
        if (selfJar == null) return;
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("linux")) return;
        String platformTag = os.contains("win") ? "windows-x64" : "macos";
        // windows natives use DLLs and mac uses dylibs
        String nativeExt = os.contains("win") ? ".dll" : ".dylib";
        try {
            // unique IDs for each natives folders
            String nativesId = Long.toHexString(selfJar.length()) + Long.toHexString(selfJar.lastModified());
            Path tempBase = Path.of(System.getProperty("java.io.tmpdir"));
            Path nativesDir = tempBase.resolve("cotsl-qt-" + nativesId);
            Path qmlDir = tempBase.resolve("cotsl-qt-qml-" + nativesId);
            // extracted natives in the CotSL temp directories
            Path sentinel = nativesDir.resolve(".cotsl-extracted");
            try (var listing = Files.list(tempBase)) {
                // clear other temp files if not used
                listing.filter(p -> {
                    String n = p.getFileName().toString();
                    return (n.startsWith("cotsl-qt-") && !n.startsWith("cotsl-qt-qml-") && !n.equals("cotsl-qt-" + nativesId))
                            || (n.startsWith("cotsl-qt-qml-") && !n.equals("cotsl-qt-qml-" + nativesId));
                }).forEach(stale -> {
                    try (var walk = Files.walk(stale)) {
                        walk.sorted(Comparator.reverseOrder()).forEach(f -> f.toFile().delete());
                    } catch (IOException ignored) {}
                });
            } catch (IOException ignored) {}
            if (Files.exists(sentinel)) {
                // reusing natives from the temp directory if possible
                String dir = nativesDir.toAbsolutePath().toString();
                String current = System.getProperty("java.library.path", "");
                System.setProperty("java.library.path", current.isEmpty() ? dir : dir + File.pathSeparator + current);
                System.out.println("[CotSL] Qt native libs reused from: " + dir);
                Path platformsDir = nativesDir.resolve("platforms");
                if (Files.isDirectory(platformsDir)) System.setProperty("cotsl.qt.platformPluginPath", platformsDir.toAbsolutePath().toString());
                if (Files.isDirectory(qmlDir)) System.setProperty("cotsl.qt.qmlImportPath", qmlDir.toAbsolutePath().toString());
                return;
            }
            Files.createDirectories(nativesDir);
            int extracted = 0;
            // finally extract natives
            try (JarFile self = new JarFile(selfJar)) {
                List<JarEntry> innerJarEntries = self.stream()
                        .filter(e -> e.getName().startsWith("META-INF/extjarjar/")
                                && e.getName().endsWith(".jar")
                                && e.getName().contains("native-" + platformTag))
                        .toList();
                if (innerJarEntries.isEmpty()) System.err.println("[CotSL] No bundled native JARs found for platform: " + platformTag);
                for (JarEntry innerJarEntry : innerJarEntries) {
                    Path tmpInner = Files.createTempFile("cotsl-native-inner-", ".jar");
                    tmpInner.toFile().deleteOnExit();
                    try (InputStream in = self.getInputStream(innerJarEntry)) {
                        Files.copy(in, tmpInner, StandardCopyOption.REPLACE_EXISTING);
                    }
                    try (JarFile innerJar = new JarFile(tmpInner.toFile())) {
                        for (
                                JarEntry e : innerJar.stream()
                                    .filter(e -> !e.isDirectory() && e.getName().endsWith(nativeExt))
                                    .toList()
                        ) {
                            String name = e.getName();
                            String fileName = name.contains("/") ? name.substring(name.lastIndexOf('/') + 1) : name;
                            Path dest = nativesDir.resolve(fileName);
                            if (!Files.exists(dest)) {
                                try (InputStream in = innerJar.getInputStream(e)) {
                                    Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                                    extracted++;
                                }
                            }
                        }
                    }
                }
                // this is where the Qt natives are located in the JAR when built
                String resourcePrefix = "META-INF/qt-natives/" + platformTag + "/";
                List<JarEntry> bundled = self.stream()
                        .filter(e -> !e.isDirectory() && e.getName().startsWith(resourcePrefix))
                        .toList();
                for (JarEntry e : bundled) {
                    String relPath = e.getName().substring(resourcePrefix.length());
                    Path dest = nativesDir.resolve(relPath);
                    Files.createDirectories(dest.getParent());
                    if (!Files.exists(dest)) {
                        try (InputStream in = self.getInputStream(e)) {
                            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                            extracted++;
                        }
                    }
                }
                // this is where the QML modules are stored
                String qmlPrefix = "META-INF/qt-qml/" + platformTag + "/";
                List<JarEntry> qmlEntries = self.stream()
                        .filter(e -> !e.isDirectory() && e.getName().startsWith(qmlPrefix))
                        .toList();
                if (!qmlEntries.isEmpty()) {
                    Files.createDirectories(qmlDir);
                    for (JarEntry e : qmlEntries) {
                        String relPath = e.getName().substring(qmlPrefix.length());
                        Path dest = qmlDir.resolve(relPath);
                        Files.createDirectories(dest.getParent());
                        try (InputStream in = self.getInputStream(e)) {
                            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                    System.setProperty("cotsl.qt.qmlImportPath", qmlDir.toAbsolutePath().toString());
                    System.out.println("[CotSL] Extracted " + qmlEntries.size() + " QML module files to: " + qmlDir);
                }
                if (!bundled.isEmpty()) System.out.println("[CotSL] Extracted " + bundled.size() + " bundled Qt runtime files");
            }
            if (extracted > 0) {
                String dir = nativesDir.toAbsolutePath().toString();
                String current = System.getProperty("java.library.path", "");
                System.setProperty("java.library.path", current.isEmpty() ? dir : dir + File.pathSeparator + current);
                System.out.println("[CotSL] Qt native libs extracted to: " + dir);
                Path platformsDir = nativesDir.resolve("platforms");
                if (Files.isDirectory(platformsDir)) System.setProperty("cotsl.qt.platformPluginPath", platformsDir.toAbsolutePath().toString());
                Files.createFile(sentinel);
            } else System.err.println("[CotSL] Warning: native JARs for " + platformTag + " were found but contained no " + nativeExt + " files.");
        } catch (Exception e) {
            System.err.println("[CotSL] Failed to extract Qt natives: " + e.getMessage());
        }
    }

    /**
     * gets the agent/mod JAR itself
     * @return this agent/mod JAR
     */
    private static File findSelf() {
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (!arg.startsWith("-javaagent:")) continue;
            String path = arg.substring("-javaagent:".length());
            int eq = path.indexOf('=');
            if (eq >= 0) path = path.substring(0, eq);
            File f = new File(path);
            if (!f.isFile()) continue;
            try (JarFile jf = new JarFile(f)) {
                boolean hasThis = jf.getEntry("com/flarelabsmc/cotsl/launch/LaunchAgent.class") != null;
                boolean hasPath = jf.stream().anyMatch(e -> e.getName().startsWith("META-INF/extjarjar/"));
                if (path.contains("Temp")) throw new Exception("Sus temp file found, skipping launcher");
                if (hasThis && hasPath) return f;
            } catch (Exception exc) {
                System.err.println("[CotSL] Failed to find agent JAR, continuing: " + exc.getMessage());
                StackTraceElement[] trace = exc.getStackTrace();
                for (StackTraceElement s : trace) System.err.println("  at " + s);
            }
        }
        return null;
    }

    enum LinuxQtState {
        HAS_QT,
        NO_QT,
        NO_LINUX
    }

    /**
     * bundling Qt natives is not necessary for most Linux operating systems, so it's best avoided.<br>
     * tells the user to install Qt6 if not found for some reason
     * @return the state of whether Qt6 is available or if this is Linux at all
     */
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

    /**
     * always called last in the premain method. relaunches the game with its own arguments set within the launcher
     * @param maxHeapMB the max heap size used when launching Minecraft
     * @throws Exception
     */
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

    /**
     * gets all the necessary arguments for relaunch
     * @param allArgs
     * @return proper program arguments for relaunch
     */
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

    /**
     * gets the memory values set within rec_mem_values.txt, based on system memory
     * @param totalRamMB
     * @return
     * @throws IOException
     */
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
