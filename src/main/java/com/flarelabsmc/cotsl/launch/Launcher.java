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

public class Launcher {
    static final CountDownLatch LAUNCH_LATCH = new CountDownLatch(1);
    static final String RELAUNCHED_PROP = "cotsl.relaunched";
    static final String MAIN_RELAUNCHED_PROP = "cotsl.mainRelaunched";

    static void initLog() {
        String phase = System.getProperty(MAIN_RELAUNCHED_PROP) != null ? "main-after-bootstrap"
                : System.getProperty(RELAUNCHED_PROP) != null ? "premain-relaunched"
                : "first-run";

        log("CotSL Launcher [" + phase + "] started at " + new Date());
        if (System.getProperty(MAIN_RELAUNCHED_PROP) != null || System.getProperty(RELAUNCHED_PROP) != null) return;
        log("    os=" + System.getProperty("os.name")
                + "  java=" + System.getProperty("java.version")
                + "  java.home=" + System.getProperty("java.home"));
    }

    static void log(String msg) {
        System.out.println("[CotSL] " + msg);
    }

    static void logWith(String msg, String division) {
        System.out.println("[CotSL-" + division + "] " + msg);
    }

    static void logErr(String msg) {
        System.err.println("[CotSL] " + msg);
    }

    static void logErr(String msg, Throwable t) {
        logErr(msg);
        t.printStackTrace(System.err);
    }

    static void logErrWith(String msg, String division) {
        System.err.println("[CotSL-" + division + "] " + msg);
    }

    static void logErrWith(String msg, String division, Throwable t) {
        logErrWith(msg, division);
        t.printStackTrace(System.err);
    }

    static void main(String[] args) throws Exception {
        initLog();

        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            logErr("Unhandled exception on thread " + thread.getName(), ex);
        });

        if (System.getProperty(MAIN_RELAUNCHED_PROP) != null) {
            mainAfterBootstrap();
            return;
        }

        LinuxQtState qtState = extendLibraryPathForQt();
        if (qtState == LinuxQtState.NO_QT) {
            logErr("Qt6 runtime not found. Crashing.");
            System.exit(1);
        }

        File self = findSelf();
        if (self == null) {
            logErr("Cannot locate self JAR. Aborting.");
            System.exit(1);
        }

        List<Path> extJars = new ArrayList<>();
        try (JarFile jar = new JarFile(self)) {
            for (
                    JarEntry e : jar.stream()
                    .filter(e ->
                            e.getName().startsWith("META-INF/extjarjar/")
                                    && e.getName().endsWith(".jar")
                    )
                    .toList()
            ) {
                Path tmp = Files.createTempFile("cotsl-ext-", ".jar");
                tmp.toFile().deleteOnExit();

                try (InputStream in = jar.getInputStream(e)) {
                    Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                }

                extJars.add(tmp);
            }
        }

        extractQtNatives(self);

        String nativesPath = System.getProperty("java.library.path", "");
        String platformPlugin = System.getProperty("cotsl.qt.platformPluginPath");
        String qmlImport = System.getProperty("cotsl.qt.qmlImportPath");
        String ldLibPath = System.getProperty("cotsl.qt.ldLibraryPath");
        boolean bundledQt = System.getProperty("cotsl.qt.bundled") != null;

        StringJoiner classpath = new StringJoiner(File.pathSeparator);
        classpath.add(self.getAbsolutePath());

        extJars.forEach(p -> classpath.add(p.toAbsolutePath().toString()));

        String java = ProcessHandle.current().info().command()
                .orElseGet(() ->
                        System.getProperty("java.home")
                                + File.separator
                                + "bin"
                                + File.separator + "java"
                );
        List<String> cmd = new ArrayList<>(List.of(
                java,
                "-D" + MAIN_RELAUNCHED_PROP + "=true",
                "-Djava.library.path=" + nativesPath,
                "-classpath", classpath.toString(),
                Launcher.class.getName()
        ));

        if (platformPlugin != null)
            cmd.add(1, "-Dcotsl.qt.platformPluginPath=" + platformPlugin);
        if (qmlImport != null)
            cmd.add(1, "-Dcotsl.qt.qmlImportPath=" + qmlImport);
        if (bundledQt)
            cmd.add(1, "-Dcotsl.qt.bundled=true");

        ProcessBuilder pb = new ProcessBuilder(cmd).inheritIO();

        if (ldLibPath != null) {
            String existing = pb.environment().getOrDefault("LD_LIBRARY_PATH", "");
            pb.environment().put(
                    "LD_LIBRARY_PATH",
                    existing.isEmpty() ?
                            ldLibPath :
                            ldLibPath + ":" + existing
            );
        }

        int exit = pb.start().waitFor();
        System.exit(exit);
    }

    private static void mainAfterBootstrap() throws Exception {
        LinuxQtState qtState = extendLibraryPathForQt();
        if (qtState == LinuxQtState.NO_QT) System.exit(1);

        log("Opening launcher window...");

        try {
            LauncherWindow.create(LAUNCH_LATCH);

            log("Window returned.");

            if (LAUNCH_LATCH.getCount() > 0) {
                log("Window closed without launch. Exiting.");
                System.exit(0);
            }
        } catch (Throwable t) {
            logErr("Launcher unavailable (" + t.getMessage() + ")", t);
            System.exit(1);
        }
        launchMinecraft();
    }

    public static boolean isWayland() {
        return System.getenv("XDG_SESSION_TYPE").equals("wayland");
    }

    /**
     * gets the agent/mod JAR itself
     * @return this agent/mod JAR
     */
    public static File findSelf() {
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (!arg.startsWith("-javaagent:")) continue;

            String path = arg.substring("-javaagent:".length());

            int eq = path.indexOf('=');
            if (eq >= 0) path = path.substring(0, eq);

            File file = new File(path);
            if (!file.isFile()) continue;

            try (JarFile jar = new JarFile(file)) {
                boolean hasThis = jar.getEntry("com/flarelabsmc/cotsl/launch/Launcher.class") != null;
                boolean hasPath = jar.stream().anyMatch(e -> e.getName().startsWith("META-INF/extjarjar/"));

                if (path.contains("Temp")) throw new Exception("Sus temp file found, skipping launcher");

                if (hasThis && hasPath) return file;
            } catch (Exception exc) {
                logErr("Failed to find agent JAR, continuing: " + exc.getMessage());

                StackTraceElement[] trace = exc.getStackTrace();
                for (StackTraceElement s : trace) logErr("  at " + s);
            }
        }
        try {
            File protectionDomain = new File(
                    Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            );
            if (protectionDomain.isFile()) return protectionDomain;
        } catch (Exception ignored) {}
        return null;
    }

    private static void launchMinecraft() throws Exception {
        InstallState.Options state = InstallState.get();
        if (state.mcDir == null) {
            logErr("No mcDir recorded, cannot launch.");
            System.exit(1);
        }

        log("Launching Minecraft directly...");

        Process mc = MinecraftLauncher.launch(
                new File(state.mcDir),
                Paths.getInstanceDir(),
                state,
                InstallManager.getReqNeoVer(),
                findSelf()
        );
        int exit = mc.waitFor();
        System.exit(exit);
    }

    /**
     * loads the JARs within the META-INF/extjarjar folder, used by the agent because NeoForge's JarJar does not apply at this stage
     */
    static void loadExtraJars(Instrumentation inst, File selfJar) throws Exception {
        if (selfJar == null) {
            logErr("Could not locate own JAR, skipping external jarjar loading (this is fatal!)");
            return;
        }

        try (JarFile self = new JarFile(selfJar)) {
            List<JarEntry> entries = self.stream()
                    .filter(e ->
                            e.getName().startsWith("META-INF/jarjar/")
                                    && e.getName().endsWith(".jar")
                    )
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
    static void extractQtNatives(File selfJar) {
        if (selfJar == null) return;

        String os = System.getProperty("os.name", "").toLowerCase();
        String platformTag;
        String nativeExt;

        if (os.contains("win")) {
            platformTag = "windows-x64";
            nativeExt = ".dll";
        } else if (os.contains("mac")) {
            platformTag = "macos";
            nativeExt = ".dylib";
        } else return;

        try {
            // unique IDs for each natives folders
            String nativesId = Long.toHexString(selfJar.length())
                    + Long.toHexString(selfJar.lastModified());
            Path tempBase = Path.of(System.getProperty("java.io.tmpdir"));
            Path nativesDir = tempBase.resolve("cotsl-qt-" + nativesId);
            Path qmlDir = tempBase.resolve("cotsl-qt-qml-" + nativesId);
            Path sentinel = nativesDir.resolve(".cotsl-extracted");

            try (var listing = Files.list(tempBase)) {
                listing.filter(p -> {
                    String n = p.getFileName().toString();

                    return (
                            n.startsWith("cotsl-qt-")
                                    && !n.startsWith("cotsl-qt-qml-")
                                    && !n.equals("cotsl-qt-" + nativesId)
                    ) || (
                            n.startsWith("cotsl-qt-qml-")
                                    && !n.equals("cotsl-qt-qml-" + nativesId)
                    );
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
                System.setProperty(
                        "java.library.path",
                        current.isEmpty() ?
                                dir :
                                dir + File.pathSeparator + current
                );

                log("Qt native libs reused from: " + dir);

                Path platformsDir = nativesDir.resolve("platforms");
                if (Files.isDirectory(platformsDir))
                    System.setProperty(
                            "cotsl.qt.platformPluginPath",
                            platformsDir.toAbsolutePath().toString()
                    );
                if (Files.isDirectory(qmlDir))
                    System.setProperty(
                            "cotsl.qt.qmlImportPath",
                            qmlDir.toAbsolutePath().toString()
                    );

                if (os.contains("linux")) {
                    System.setProperty("cotsl.qt.bundled", "true");
                    System.setProperty("cotsl.qt.ldLibraryPath", dir);
                }

                return;
            }

            Files.createDirectories(nativesDir);

            int extracted = 0;

            try (JarFile self = new JarFile(selfJar)) {
                List<JarEntry> innerJarEntries = self.stream()
                        .filter(e -> e.getName().startsWith("META-INF/extjarjar/")
                                && e.getName().endsWith(".jar")
                                && e.getName().contains("native-" + platformTag))
                        .toList();
                if (innerJarEntries.isEmpty())
                    logErr("No bundled native JARs found for platform: " + platformTag);

                for (JarEntry innerJarEntry : innerJarEntries) {
                    Path tmpInner = Files.createTempFile("cotsl-native-inner-", ".jar");
                    tmpInner.toFile().deleteOnExit();

                    try (InputStream in = self.getInputStream(innerJarEntry)) {
                        Files.copy(in, tmpInner, StandardCopyOption.REPLACE_EXISTING);
                    }

                    try (JarFile innerJar = new JarFile(tmpInner.toFile())) {
                        for (JarEntry e : innerJar.stream()
                                .filter(e ->
                                        !e.isDirectory() && isNativeFile(e.getName(), nativeExt)
                                )
                                .toList()) {
                            String name = e.getName();
                            String fileName = name.contains("/") ?
                                    name.substring(name.lastIndexOf('/') + 1) : name;
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

                    log("Extracted " + qmlEntries.size() + " QML module files to: " + qmlDir);
                }
                if (!bundled.isEmpty())
                    log("Extracted " + bundled.size() + " bundled Qt runtime files");
            }
            if (extracted > 0) {
                if (os.contains("linux")) createLinuxSoSymlinks(nativesDir);

                String dir = nativesDir.toAbsolutePath().toString();
                String current = System.getProperty("java.library.path", "");
                System.setProperty(
                        "java.library.path",
                        current.isEmpty() ? dir : dir + File.pathSeparator + current
                );

                log("Qt native libs extracted to: " + dir);

                Path platformsDir = nativesDir.resolve("platforms");
                if (Files.isDirectory(platformsDir))
                    System.setProperty(
                            "cotsl.qt.platformPluginPath",
                            platformsDir.toAbsolutePath().toString()
                    );

                if (os.contains("linux")) {
                    System.setProperty("cotsl.qt.bundled", "true");
                    System.setProperty("cotsl.qt.ldLibraryPath", dir);
                }

                Files.createFile(sentinel);
            } else logErr(
                    "Warning: native JARs for "
                    + platformTag
                    + " were found but contained no native files."
            );
        } catch (Exception e) {
            logErr("Failed to extract Qt natives: " + e.getMessage());
        }
    }

    private static boolean isNativeFile(String name, String ext) {
        if (ext.equals(".so")) return name.endsWith(".so") || name.contains(".so.");
        return name.endsWith(ext);
    }

    private static void createLinuxSoSymlinks(Path dir) {
        try (var stream = Files.list(dir)) {
            stream.filter(p -> {
                String n = p.getFileName().toString();
                return n.contains(".so.") && n.matches(".*\\.so\\.\\d+\\.\\d+\\.\\d+");
            }).forEach(p -> {
                String name = p.getFileName().toString();
                int soIdx = name.lastIndexOf(".so.");
                String base = name.substring(0, soIdx);
                String soVersion = name.substring(soIdx + 4);
                String major = soVersion.split("\\.")[0];

                try {
                    Path majorLink = dir.resolve(base + ".so." + major);
                    Path bareLink = dir.resolve(base + ".so");

                    if (!Files.exists(majorLink)) Files.createSymbolicLink(majorLink, p.getFileName());
                    if (!Files.exists(bareLink)) Files.createSymbolicLink(bareLink, p.getFileName());
                } catch (IOException ignored) {}
            });
        } catch (IOException e) {
            logErr("Could not create .so symlinks: " + e.getMessage());
        }
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
    static LinuxQtState extendLibraryPathForQt() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("linux")) return LinuxQtState.NO_LINUX;
        if (System.getProperty("cotsl.qt.bundled") != null) return LinuxQtState.HAS_QT;

        String[] candidates = {
                "/usr/lib",
                "/usr/lib/x86_64-linux-gnu",
                "/usr/lib64",
                "/usr/local/lib",
        };

        String current = System.getProperty("java.library.path", "");

        for (String dir : candidates) {
            if (new File(dir, "libQt6Core.so.6").exists()) {
                String[] versionedLibs = new File(dir).list((d, n) -> n.startsWith("libQt6Core.so.6."));

                if (versionedLibs != null && versionedLibs.length > 0) {
                    String qtVer = versionedLibs[0].replace("libQt6Core.so.", "");
                    if (!qtVer.startsWith("6.11."))
                        logErr(
                                "System Qt "
                                + qtVer
                                + " may not match QtJambi 6.11.x. Launcher UI may fail."
                                + "Build the linux-x64 JAR with QTDIR set to bundle Qt 6.11."
                        );
                }

                System.setProperty("java.library.path", current.isEmpty() ? dir : current + File.pathSeparator + dir);

                return LinuxQtState.HAS_QT;
            }
        }

        logErr("Qt6 runtime not found. This is fatal.");
        logErr("To fix this, install Qt6:");

        if (new File("/usr/bin/pacman").exists())
            logErr("sudo pacman -S qt6-base qt6-declarative");
        else if (new File("/usr/bin/apt").exists())
            logErr("sudo apt install libqt6core6t64 libqt6quick6 libqt6qml6 libqt6widgets6t64");
        else if (new File("/usr/bin/dnf").exists())
            logErr("sudo dnf install qt6-qtbase qt6-qtdeclarative");
        else if (new File("/usr/bin/zypper").exists())
            logErr("sudo zypper install libQt6Core6 libQt6Quick6 libQt6Qml6");
        else
            logErr("Install qt6-base and qt6-declarative via your package manager.");

        return LinuxQtState.NO_QT;
    }

    public static long getTotalSystemRamMB() {
        try {
            OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            return os.getTotalMemorySize() / (1024 * 1024);
        } catch (Exception e) { return -1; }
    }

    /**
     * gets the memory values set within rec_mem_values.txt, based on system memory
     */
    public static long computeMaxHeap(long totalRamMB) throws IOException {
        InputStream is = Launcher.class.getResourceAsStream("/rec_mem_values.txt");
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

            for (long[] entry : entries)
                if (totalRamMB < entry[0]) return entry[1];

            return entries.getLast()[1];
        }
    }
}
