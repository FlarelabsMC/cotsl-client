package com.flarelabsmc.cotsl.launch;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.jar.JarFile;

import static com.flarelabsmc.cotsl.launch.LaunchAgent.*;

public class InstallManager {

    public static void runInstallIfNeeded() throws Exception {
        InstallState.Options state = InstallState.get();
        log("[CotSL] Resolving Minecraft directory...");
        File mcDir = Paths.resolveMcDir(state);
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
            deploySelf(Paths.getInstanceDir());
            state.inSelfVer = reqSelfVer;
        }
        state.mcDir = mcDir.getAbsolutePath();
        state.save();
    }

    private static void installNeoForge(String version, File mcDir) throws Exception {
//        String url = String.format("https://maven.neoforged.net/releases/net/neoforged/neoforge/%s/neoforge-%s-installer.jar", version, version);
//        File installer = File.createTempFile("neoforge-installer-", ".jar");
//        installer.deleteOnExit();
//        log("[CotSL] Downloading NeoForge installer from: " + url);
//        try (InputStream in = new URL(url).openStream(); FileOutputStream out = new FileOutputStream(installer)) {
//            in.transferTo(out);
//        }
//        String java = ProcessHandle.current().info().command().orElseGet(() -> System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
//        int exit = new ProcessBuilder(java, "-jar", installer.getAbsolutePath(), "--installClient", mcDir.getAbsolutePath())
//                .inheritIO()
//                .start()
//                .waitFor();
//
//        if (exit != 0) throw new RuntimeException("NeoForge installer failed with exit code: " + exit);
        NeoForgeInstaller.install(version, mcDir, LaunchAgent::log);
    }


    public static void deploySelf(File mcDir) throws Exception {
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

    public static String getReqNeoVer() throws Exception {
        try (var is = LaunchAgent.class.getResourceAsStream("/neo_version.txt")) {
            if (is == null) throw new FileNotFoundException("neo_version.txt not found in JAR");
            return new String(is.readAllBytes()).trim();
        }
    }

    public static String getReqSelfVer() throws Exception {
        try (var is = LaunchAgent.class.getResourceAsStream("/cotsl_version.txt")) {
            if (is == null) throw new FileNotFoundException("cotsl_version.txt not found in JAR");
            return new String(is.readAllBytes()).trim();
        }
    }
}
