package com.flarelabsmc.cotsl.launch;

import com.sun.management.OperatingSystemMXBean;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.java.StepMCProfile;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.net.URL;
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
 */
public class LaunchAgent {
    static final CountDownLatch LAUNCH_LATCH = new CountDownLatch(1);
    private static final String RELAUNCHED_PROP = "cotsl.relaunched";
    private static final String MAIN_RELAUNCHED_PROP = "cotsl.mainRelaunched";
    private static final String INSTALL_STATE_FILE = "install_state.json";
    private static PrintWriter logWriter = null;

    private static void initLog() {
        try {
            File dir = getInstallStateFile().getParentFile();
            dir.mkdirs();
            File logFile = new File(dir, "cotsl-launch.log");
            logWriter = new PrintWriter(new FileWriter(logFile, true), true);
            String phase = System.getProperty(MAIN_RELAUNCHED_PROP) != null ? "main-after-bootstrap"
                    : System.getProperty(RELAUNCHED_PROP) != null ? "premain-relaunched"
                    : "first-run";
            log("CotSL LaunchAgent [" + phase + "] started at " + new java.util.Date());
            log("    os=" + System.getProperty("os.name")
                    + "  java=" + System.getProperty("java.version")
                    + "  java.home=" + System.getProperty("java.home"));
        } catch (IOException e) {
            System.err.println("[CotSL] Could not open log file: " + e.getMessage());
        }
    }

    private static void log(String msg) {
        System.out.println(msg);
        if (logWriter != null) logWriter.println(msg);
    }

    private static void logErr(String msg) {
        System.err.println(msg);
        if (logWriter != null) logWriter.println("[ERROR] " + msg);
    }

    private static void logErr(String msg, Throwable t) {
        logErr(msg);
        if (logWriter != null) t.printStackTrace(logWriter);
        else t.printStackTrace(System.err);
    }

    public static void main(String[] args) throws Exception {
        initLog();
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            logErr("[CotSL] Unhandled exception on thread " + thread.getName(), ex);
            if (logWriter != null) logWriter.flush();
        });
        if (System.getProperty(MAIN_RELAUNCHED_PROP) != null) {
            mainAfterBootstrap();
            return;
        }
        LinuxQtState qtState = extendLibraryPathForQt();
        if (qtState == LinuxQtState.NO_QT) {
            logErr("[CotSL] Qt6 runtime not found. Crashing.");
            System.exit(1);
        }
        File self = findSelf();
        if (self == null) {
            logErr("[CotSL] Cannot locate self JAR. Aborting.");
            System.exit(1);
        }
        List<Path> extJars = new ArrayList<>();
        try (JarFile jar = new JarFile(self)) {
            for (JarEntry e : jar.stream()
                    .filter(e -> e.getName().startsWith("META-INF/extjarjar/") && e.getName().endsWith(".jar"))
                    .toList()) {
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
                .orElseGet(() -> System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        List<String> cmd = new ArrayList<>(List.of(
                java,
                "-D" + MAIN_RELAUNCHED_PROP + "=true",
                "-Djava.library.path=" + nativesPath,
                "-classpath", classpath.toString(),
                LaunchAgent.class.getName()
        ));
        if (platformPlugin != null) cmd.add(1, "-Dcotsl.qt.platformPluginPath=" + platformPlugin);
        if (qmlImport != null) cmd.add(1, "-Dcotsl.qt.qmlImportPath=" + qmlImport);
        if (bundledQt) cmd.add(1, "-Dcotsl.qt.bundled=true");
        ProcessBuilder pb = new ProcessBuilder(cmd).inheritIO();
        if (ldLibPath != null) {
            String existing = pb.environment().getOrDefault("LD_LIBRARY_PATH", "");
            pb.environment().put("LD_LIBRARY_PATH", existing.isEmpty() ? ldLibPath : ldLibPath + ":" + existing);
        }
        int exit = pb.start().waitFor();
        System.exit(exit);
    }

    public static void premain(String agentArgs, Instrumentation inst) throws Exception {
        initLog();
        if (System.getProperty(RELAUNCHED_PROP) != null) return;
        if (System.getProperty("cotsl.minecraft.launch") != null) {
            File selfJar = findSelf();
            loadExtraJars(inst, selfJar);
            extractQtNatives(selfJar);
            return;
        }
        LinuxQtState qtState = extendLibraryPathForQt();
        if (qtState == LinuxQtState.NO_QT) {
            logErr("[CotSL] Qt6 runtime not found, cannot launch launcher window. Crashing.");
            System.exit(1);
            return;
        }
        File selfJar = findSelf();
        loadExtraJars(inst, selfJar);
        extractQtNatives(selfJar);
        runInstallIfNeeded();
        try {
            LauncherWindow.create(LAUNCH_LATCH);
            if (LAUNCH_LATCH.getCount() > 0) System.exit(0);
        } catch (Throwable t) {
            logErr("[CotSL] Launcher unavailable (" + t.getMessage() + "), launching directly");
            return;
        }
        tryRelaunch();
    }

    private static void mainAfterBootstrap() throws Exception {
        LinuxQtState qtState = extendLibraryPathForQt();
        if (qtState == LinuxQtState.NO_QT) System.exit(1);
        log("[CotSL] Running install check...");
        try {
            runInstallIfNeeded();
        } catch (Throwable t) {
            logErr("[CotSL] runInstallIfNeeded() failed", t);
            System.exit(1);
        }
        log("[CotSL] Opening launcher window...");
        try {
            LauncherWindow.create(LAUNCH_LATCH);
            log("[CotSL] Window returned. Latch count=" + LAUNCH_LATCH.getCount());
            if (LAUNCH_LATCH.getCount() > 0) {
                log("[CotSL] Window closed without launch. Exiting.");
                System.exit(0);
            }
        } catch (Throwable t) {
            logErr("[CotSL] Launcher unavailable (" + t.getMessage() + "), falling back to headless mode.", t);
            try {
                authIfNeeded();
            } catch (Exception e) {
                logErr("[CotSL] Headless auth failed", e);
                System.exit(1);
            }
        }
        launchMinecraft();
    }


    private static void launchMinecraft() throws Exception {
        InstallState state = InstallState.load(getInstallStateFile());
        if (state.mcDir == null) {
            logErr("[CotSL] No mcDir recorded, cannot launch.");
            System.exit(1);
        }
        log("[CotSL] Launching Minecraft directly...");
        MinecraftLauncher.launch(new File(state.mcDir), getInstanceDir(), state, getReqNeoVer(), findSelf());
        System.exit(0);
    }

    static File getInstanceDir() {
        return new File(getInstallStateFile().getParentFile(), "instance");
    }

    private static void runInstallIfNeeded() throws Exception {
        log("[CotSL] Loading install state...");
        File stateFile = getInstallStateFile();
        InstallState state = InstallState.load(stateFile);
        log("[CotSL] Resolving Minecraft directory...");
        File mcDir = resolveMcDir(state);
        log("[CotSL] mcDir=" + mcDir);
        if (mcDir == null) {
            logErr("[CotSL] Could not find Minecraft directory. Aborting install.");
            System.exit(1);
            return;
        }
        String reqNeoVer = getReqNeoVer();
        String reqSelfVer = getReqSelfVer();
        log("[CotSL] Required: NeoForge=" + reqNeoVer + "  self=" + reqSelfVer);
        log("[CotSL] Installed: NeoForge=" + state.inNeoVer + "  self=" + state.inSelfVer);
        boolean needsToInstallNeo = !reqNeoVer.equals(state.inNeoVer)
                || !new File(mcDir, "versions/neoforge-" + reqNeoVer).isDirectory();
        boolean selfNeedsUpdate = !reqSelfVer.equals(state.inSelfVer);
        log("[CotSL] needsNeo=" + needsToInstallNeo + "  needsSelf=" + selfNeedsUpdate);
        if (needsToInstallNeo) {
            log("[CotSL] Installing NeoForge" + reqNeoVer);
            installNeoForge(reqNeoVer, mcDir);
            state.inNeoVer = reqNeoVer;
        }
        if (selfNeedsUpdate || needsToInstallNeo) {
            log("[CotSL] Deploying mod JAR");
            deploySelf(getInstanceDir());
            state.inSelfVer = reqSelfVer;
        }
        state.mcDir = mcDir.getAbsolutePath();
        state.save(stateFile);
    }

    private static void installNeoForge(String version, File mcDir) throws Exception {
        String url = String.format("https://maven.neoforged.net/releases/net/neoforged/neoforge/%s/neoforge-%s-installer.jar", version, version);
        File installer = File.createTempFile("neoforge-installer-", ".jar");
        installer.deleteOnExit();
        log("[CotSL] Downloading NeoForge installer from: " + url);
        try (InputStream in = new URL(url).openStream(); FileOutputStream out = new FileOutputStream(installer)) {
            in.transferTo(out);
        }
        String java = ProcessHandle.current().info().command().orElseGet(() -> System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        int exit = new ProcessBuilder(java, "-jar", installer.getAbsolutePath(), "--installClient", mcDir.getAbsolutePath())
                .inheritIO()
                .start()
                .waitFor();

        if (exit != 0) throw new RuntimeException("NeoForge installer failed with exit code: " + exit);
    }

    private static void deploySelf(File mcDir) throws Exception {
        File selfJar = findSelf();
        if (selfJar == null) throw new IllegalStateException("Cannot find self to deploy into mods folder");
        File modsDir = new File(mcDir, "mods");
        modsDir.mkdirs();
        File[] old = modsDir.listFiles(f -> f.getName().startsWith("cotsl-") && f.getName().endsWith(".jar"));
        if (old != null) for (File f : old) f.delete();
        File dest = new File(modsDir, selfJar.getName());
        Files.copy(selfJar.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        log("[CotSL] Deployed self to:" + dest);
    }

    private static String getReqNeoVer() throws Exception {
        try (var is = LaunchAgent.class.getResourceAsStream("/neo_version.txt")) {
            if (is == null) throw new FileNotFoundException("neo_version.txt not found in JAR");
            return new String(is.readAllBytes()).trim();
        }
    }

    private static String getReqSelfVer() throws Exception {
        try (var is = LaunchAgent.class.getResourceAsStream("/cotsl_version.txt")) {
            if (is == null) throw new FileNotFoundException("cotsl_version.txt not found in JAR");
            return new String(is.readAllBytes()).trim();
        }
    }

    private static File resolveMcDir(InstallState state) {
        if (state.mcDir != null) {
            File saved = new File(state.mcDir);
            if (saved.isDirectory()) return saved;
        }
        File detected = getMcDir();
        if (detected.isDirectory() && launcherProfilesExist(detected)) return detected;
        System.setProperty("cotsl.install.needsMcDir", "true");
        try {
            LauncherWindow.create(LAUNCH_LATCH);
        } catch (Throwable e) {
            log("[CotSL] Could not show directory picker (" + e.getMessage() + "), using default");
        }
        String chosen = System.getProperty("cotsl.install.chosenMcDir");
        if (chosen != null) return new File(chosen);
        detected.mkdirs();
        return detected;
    }

    private static boolean launcherProfilesExist(File mcDir) {
        return new File(mcDir, "launcher_profiles.json").exists() || new File(mcDir, "launcher_profiles_microsoft_store.json").exists();
    }

    private static File getMcDir() {
        if (System.getenv("mcdir") != null) return new File(System.getenv("mcdir"));
        String home = System.getProperty("user.home", ".");
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win") && System.getenv("APPDATA") != null)
            return new File(System.getenv("APPDATA"), ".minecraft");
        if (os.contains("mac")) return new File(home, "Library/Application Support/minecraft");
        File c = new File(home, ".minecraft");
        if (!c.exists() && os.contains("linux")) {
            File flatpak = new File(home, ".var/app/com.mojang.Minecraft/.minecraft");
            if (flatpak.exists()) return flatpak;
        }
        return c;
    }

    static File getInstallStateFile() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String home = System.getProperty("user.home", ".");
        File dir;
        if (os.contains("win") && System.getenv("APPDATA") != null) dir = new File(System.getenv("APPDATA"), ".cotsl");
        else if (os.contains("mac")) dir = new File(home, "Library/Application Support/.cotsl");
        else dir = new File(home, ".cotsl");
        dir.mkdirs();
        return new File(dir, INSTALL_STATE_FILE);
    }

    /**
     * after the agent overrides launch, it grabs the arguments from when it tried to launch as Minecraft, and relaunches the game with proper JVM arguments
     * @throws IOException
     */
    private static void tryRelaunch() throws IOException {
        long totalRamMB = getTotalSystemRamMB();
        long recommended = totalRamMB > 0 ? computeMaxHeap(totalRamMB) : Runtime.getRuntime().maxMemory() / (1024 * 1024);
        try { doRelaunch(recommended); }
        catch (Exception e) { logErr("[CotSL] Could not relaunch with stock JVM args, continuing as is: " + e.getMessage()); }
    }

    /**
     * loads the JARs within the META-INF/extjarjar folder, used by the agent because NeoForge's JarJar does not apply at this stage
     * @param inst
     * @param selfJar
     * @throws Exception
     */
    private static void loadExtraJars(Instrumentation inst, File selfJar) throws Exception {
        if (selfJar == null) {
            logErr("[CotSL] Could not locate own JAR, skipping extjarjar loading (this is fatal!)");
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
        String platformTag;
        String nativeExt;
        if (os.contains("win")) {
            platformTag = "windows-x64";
            nativeExt = ".dll";
        } else if (os.contains("mac")) {
            platformTag = "macos";
            nativeExt = ".dylib";
        } else if (os.contains("linux")) {
            platformTag = "linux-x64";
            nativeExt = ".so";
        } else return;
        try {
            // unique IDs for each natives folders
            String nativesId = Long.toHexString(selfJar.length()) + Long.toHexString(selfJar.lastModified());
            Path tempBase = Path.of(System.getProperty("java.io.tmpdir"));
            Path nativesDir = tempBase.resolve("cotsl-qt-" + nativesId);
            Path qmlDir = tempBase.resolve("cotsl-qt-qml-" + nativesId);
            Path sentinel = nativesDir.resolve(".cotsl-extracted");
            try (var listing = Files.list(tempBase)) {
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
                log("[CotSL] Qt native libs reused from: " + dir);
                Path platformsDir = nativesDir.resolve("platforms");
                if (Files.isDirectory(platformsDir)) System.setProperty("cotsl.qt.platformPluginPath", platformsDir.toAbsolutePath().toString());
                if (Files.isDirectory(qmlDir)) System.setProperty("cotsl.qt.qmlImportPath", qmlDir.toAbsolutePath().toString());
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
                if (innerJarEntries.isEmpty()) logErr("[CotSL] No bundled native JARs found for platform: " + platformTag);
                for (JarEntry innerJarEntry : innerJarEntries) {
                    Path tmpInner = Files.createTempFile("cotsl-native-inner-", ".jar");
                    tmpInner.toFile().deleteOnExit();
                    try (InputStream in = self.getInputStream(innerJarEntry)) {
                        Files.copy(in, tmpInner, StandardCopyOption.REPLACE_EXISTING);
                    }
                    try (JarFile innerJar = new JarFile(tmpInner.toFile())) {
                        for (JarEntry e : innerJar.stream()
                                .filter(e -> !e.isDirectory() && isNativeFile(e.getName(), nativeExt))
                                .toList()) {
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
                    log("[CotSL] Extracted " + qmlEntries.size() + " QML module files to: " + qmlDir);
                }
                if (!bundled.isEmpty()) log("[CotSL] Extracted " + bundled.size() + " bundled Qt runtime files");
            }
            if (extracted > 0) {
                if (os.contains("linux")) createLinuxSoSymlinks(nativesDir);
                String dir = nativesDir.toAbsolutePath().toString();
                String current = System.getProperty("java.library.path", "");
                System.setProperty("java.library.path", current.isEmpty() ? dir : dir + File.pathSeparator + current);
                log("[CotSL] Qt native libs extracted to: " + dir);
                Path platformsDir = nativesDir.resolve("platforms");
                if (Files.isDirectory(platformsDir)) System.setProperty("cotsl.qt.platformPluginPath", platformsDir.toAbsolutePath().toString());
                if (os.contains("linux")) {
                    System.setProperty("cotsl.qt.bundled", "true");
                    System.setProperty("cotsl.qt.ldLibraryPath", dir);
                }
                Files.createFile(sentinel);
            } else logErr("[CotSL] Warning: native JARs for " + platformTag + " were found but contained no native files.");
        } catch (Exception e) {
            logErr("[CotSL] Failed to extract Qt natives: " + e.getMessage());
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
            logErr("[CotSL] Could not create .so symlinks: " + e.getMessage());
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
                logErr("[CotSL] Failed to find agent JAR, continuing: " + exc.getMessage());
                StackTraceElement[] trace = exc.getStackTrace();
                for (StackTraceElement s : trace) logErr("  at " + s);
            }
        }
        try {
            File protectionDomain = new File(
                    LaunchAgent.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            );
            if (protectionDomain.isFile()) return protectionDomain;
        } catch (Exception ignored) {}
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
                    if (!qtVer.startsWith("6.10.")) logErr("[CotSL] System Qt " + qtVer + " may not match QtJambi 6.10.x. Launcher UI may fail. Build the linux-x64 JAR with QTDIR set to bundle Qt 6.10.");
                }
                System.setProperty("java.library.path", current.isEmpty() ? dir : current + File.pathSeparator + dir);
                return LinuxQtState.HAS_QT;
            }
        }
        logErr("[CotSL] Qt6 runtime not found. This is fatal.");
        logErr("[CotSL] To fix this, install Qt6:");
        if (new File("/usr/bin/pacman").exists()) logErr("[CotSL] sudo pacman -S qt6-base qt6-declarative");
        else if (new File("/usr/bin/apt").exists()) logErr("[CotSL] sudo apt install libqt6core6t64 libqt6quick6 libqt6qml6 libqt6widgets6t64");
        else if (new File("/usr/bin/dnf").exists()) logErr("[CotSL] sudo dnf install qt6-qtbase qt6-qtdeclarative");
        else if (new File("/usr/bin/zypper").exists()) logErr("[CotSL] sudo zypper install libQt6Core6 libQt6Quick6 libQt6Qml6");
        else logErr("[CotSL] Install qt6-base and qt6-declarative via your package manager.");
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
        // apparently macos crashes without this
        if (System.getProperty("os.name").toLowerCase().contains("mac")) args.add("-XstartOnFirstThread");
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

    private static void authIfNeeded() throws Exception {
        File stateFile = getInstallStateFile();
        InstallState state = InstallState.load(stateFile);
        if (state.authToken != null && System.currentTimeMillis() < state.authExpiry) {
            log("[CotSL] Using existing auth for " + state.playerName);
            return;
        }
        log("[CotSL] Starting Microsoft sign-in...");
        HttpClient httpClient = new HttpClient();
        StepFullJavaSession.FullJavaSession session = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.getFromInput(
                httpClient,
                new StepMsaDeviceCode.MsaDeviceCodeCallback(code -> {
                    log("[CotSL] Sign in at: " + code.getDirectVerificationUri());
                    log("[CotSL] Or visit https://www.microsoft.com/link and enter: " + code.getUserCode());
                })
        );
        StepMCProfile.MCProfile profile = session.getMcProfile();
        state.authToken = profile.getMcToken().getAccessToken();
        state.playerName = profile.getName();
        state.playerUuid = profile.getId().toString();
        state.authExpiry = profile.getMcToken().getExpireTimeMs();
        state.save(stateFile);
        log("[CotSL] Signed in as: " + state.playerName);
    }
}
