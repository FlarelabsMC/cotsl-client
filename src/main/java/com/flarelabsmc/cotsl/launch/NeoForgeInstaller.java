package com.flarelabsmc.cotsl.launch;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import static com.flarelabsmc.cotsl.launch.LaunchAgent.log;

public class NeoForgeInstaller {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String NEO_MAVEN = "https://maven.neoforged.net/releases/";
    private static final String MC_MAVEN = "https://libraries.minecraft.net/";
    private static final String CENTRAL = "https://repo1.maven.org/maven2/";


    public static void verifyInstallation(File neoInstaller, String neoVersion, File mcDir, VersionJson vanillaVersionJson) throws Exception {
        File neoClientJar = Paths.getLibraryDir(mcDir).toPath()
                .resolve("net", "neoforged", "neoforge", neoVersion)
                .resolve("neoforge-" + neoVersion + "-universal.jar").toFile();

        if (neoClientJar.exists() && neoClientJar.length() > 0) {
            return;
        }

        install(neoInstaller, neoVersion, mcDir, vanillaVersionJson, progress -> {
            log("[CotSL-NeoForge] " + progress);
        });
    }

    public static void install(File neoInstaller, String neoVersion, File mcDir, VersionJson vanillaVersionJson, Consumer<String> progress) throws Exception {
        if (neoInstaller == null) {
            neoInstaller = downloadInstaller(neoVersion);
        }


        try (JarFile jar = new JarFile(neoInstaller)) {
            progress.accept("Reading install profile...");
            InstallProfile profile = MAPPER.readValue(
                    jar.getInputStream(jar.getEntry("install_profile.json")),
                    InstallProfile.class
            );

            progress.accept("Installing version profile...");
            placeNeoVersionJson(jar, profile.json, neoVersion, mcDir);
            progress.accept("Downloading Minecraft " + profile.minecraft + "...");
            File vanillaJar = downloadVanillaJar(profile.minecraft, mcDir, vanillaVersionJson, progress);
            progress.accept("Downloading NeoForge installer libraries...");
            downloadLibraries(profile.libraries, mcDir, progress);
            Map<String, File> dataMap = resolveData(jar, profile, mcDir, progress);
            dataMap.put("MINECRAFT_JAR", vanillaJar);
            runProcessors(profile, dataMap, mcDir, progress);
        }
        progress.accept("NeoForge installation complete.");
    }

    /// Downloads the NeoForge installer jar for the corresponding `neoVersion` (for example `26.1.2.19-beta`) and
    /// returns the temp file it was saved to
    public static File downloadInstaller(String neoVersion) throws IOException, InterruptedException {
        String installerUrl = String.format(
                "https://maven.neoforged.net/releases/net/neoforged/neoforge/%s/neoforge-%s-installer.jar",
                neoVersion, neoVersion);

        File installerJar = File.createTempFile("neoforge-installer-", ".jar");
        installerJar.deleteOnExit();

        log("[CotSL-NeoForge] Downloading NeoForge " + neoVersion + " installer...");
        Networking.downloadFile(URI.create(installerUrl), installerJar.toPath());

        return installerJar;
    }

    public static VersionJson getNeoVersionJson(String neoVersion, File mcDir) {
        File expectedPath = Paths.getNeoVersionJsonPath(mcDir, neoVersion);

        if (!expectedPath.exists()) return null;

        try {
            return MAPPER.readValue(expectedPath, VersionJson.class);
        } catch (Exception e) {
            log("[CotSL-NeoForge] Failed to read NeoForge version.json: " + e);
            return null;
        }
    }

    public static void placeNeoVersionJson(JarFile jar, String jsonPath, String neoVer, File mcDir) throws IOException {
        String entryName = jsonPath.startsWith("/") ? jsonPath.substring(1) : jsonPath;
        ZipEntry entry = jar.getEntry(entryName);

        if (entry == null) throw new FileNotFoundException("version.json not found in installer at: " + entryName);
        File dest = Paths.getNeoVersionJsonPath(mcDir, neoVer);
        dest.getParentFile().mkdirs();
        try (InputStream in = jar.getInputStream(entry)) {
            Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static File downloadVanillaJar(String mcVersion, File mcDir, VersionJson vanillaVersionJson, Consumer<String> progress) throws Exception {
        File dest = new File(Paths.getVersionDir(mcDir), mcVersion + "/" + mcVersion + ".jar");
        if (dest.exists()) return dest;

        String jarUrl = vanillaVersionJson.downloads.client.url;
        String jarSha1 = vanillaVersionJson.downloads.client.sha1;
        dest.getParentFile().mkdirs();

        progress.accept("Downloading client jar from " + jarUrl);
        Networking.downloadFile(URI.create(jarUrl), dest.toPath());
        Paths.sha1Matches(dest, jarSha1);

        return dest;
    }


    static void downloadLibraries(List<VersionJson.Library> libs, File mcDir, Consumer<String> progress) throws Exception {
        if (libs == null) return;
        for (VersionJson.Library lib : libs) {
            if (lib.downloads != null && lib.downloads.artifact != null) {
                String path = lib.downloads.artifact.path;
                String url = lib.downloads.artifact.url;
                String sha1 = lib.downloads.artifact.sha1;
                if (path == null || url == null) continue;
                File dest = new File(mcDir, "libraries/" + path);
                if (dest.exists() && sha1Matches(dest, sha1)) continue;
                dest.getParentFile().mkdirs();
                progress.accept("  lib: " + dest.getName());
                downloadFile(url, dest, sha1);
            } else if (lib.name != null) {
                File dest = mavenFile(mcDir, lib.name, null);
                if (dest.exists()) continue;
                dest.getParentFile().mkdirs();
                String url = mavenUrl(NEO_MAVEN, lib.name, null);
                try { downloadFile(url, dest, null); }
                catch (IOException e) {
                    url = mavenUrl(CENTRAL, lib.name, null);
                    downloadFile(url, dest, null);
                }
            }
        }
    }

    private static Map<String, File> resolveData(JarFile jar, InstallProfile profile, File mcDir, Consumer<String> progress) throws Exception {
        Map<String, File> dataMap = new HashMap<>();
        if (profile.data == null) return dataMap;
        File tempData = Files.createTempDirectory("neoforge-data-").toFile();
        tempData.deleteOnExit();
        for (Map.Entry<String, InstallProfile.DataValue> entry : profile.data.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue().client;
            if (val == null) continue;
            if (val.startsWith("/")) {
                String entryName = val.substring(1);
                ZipEntry je = jar.getEntry(entryName);
                if (je == null) continue;
                File out = new File(tempData, entryName.replace('/', '_'));
                try (InputStream in = jar.getInputStream(je)) {
                    Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                dataMap.put(key, out);
            } else if (val.startsWith("[") && val.endsWith("]")) {
            String coords = val.substring(1, val.length() - 1);
            File lib = mavenFile(mcDir, coords, null);
            if (!lib.exists()) {
                lib.getParentFile().mkdirs();
                progress.accept("  data: " + lib.getName());
                try {
                    downloadFile(mavenUrl(NEO_MAVEN, coords, null), lib, null);
                } catch (IOException e1) {
                    try {
                        downloadFile(mavenUrl(CENTRAL, coords, null), lib, null);
                    } catch (IOException e2) {
                        // not available on any repo
                        // the processor will generate this file
                    }
                }
            }
            dataMap.put(key, lib);
        }

    }
        return dataMap;
    }

    private static void runProcessors(
            InstallProfile profile, Map<String, File> dataMap,
            File mcDir, Consumer<String> progress
    ) throws Exception {
        if (profile.processors == null) return;

        System.out.println(profile.processors.size());

        for (InstallProfile.Processor proc : profile.processors) {
            if (proc.sides != null && !proc.sides.contains("client")) continue;
            if (outputsUpToDate(proc.outputs, dataMap, mcDir)) continue;

            progress.accept("Running processor: " + proc.jar);
            List<URL> cpUrls = new ArrayList<>();
            cpUrls.add(mavenFile(mcDir, proc.jar, null).toURI().toURL());
            if (proc.classpath != null) {
                for (String dep : proc.classpath)
                    cpUrls.add(mavenFile(mcDir, dep, null).toURI().toURL());
            }
            String[] args = resolveArgs(proc.args, dataMap, mcDir);
            File procJar = mavenFile(mcDir, proc.jar, null);
            String mainClass;
            try (JarFile jf = new JarFile(procJar)) {
                mainClass = jf.getManifest().getMainAttributes().getValue("Main-Class");
            }
            if (mainClass == null)
                throw new IllegalStateException("Processor JAR has no Main-Class: " + proc.jar);

            ClassLoader parent = ClassLoader.getPlatformClassLoader();
            try (var processorClassLoader = new URLClassLoader(cpUrls.toArray(new URL[0]), parent)) {
                Thread t = new Thread(() -> {
                    try {
                        ClassLoader old = Thread.currentThread().getContextClassLoader();
                        Thread.currentThread().setContextClassLoader(processorClassLoader);
                        Class<?> clazz = Class.forName(mainClass, true, processorClassLoader);
                        clazz.getMethod("main", String[].class).invoke(null, (Object) args);
                        Thread.currentThread().setContextClassLoader(old);
                    } catch (Exception e) {
                        throw new RuntimeException("Processor " + proc.jar + " failed", e);
                    }
                });
                t.setContextClassLoader(processorClassLoader);
                t.start();
                t.join();
                if (t.getState() == Thread.State.TERMINATED) {
                    //
                }
            }
        }
    }

    private static String[] resolveArgs(List<String> args, Map<String, File> dataMap, File mcDir) {
        if (args == null) return new String[0];
        return args.stream().map(arg -> {
            if (arg.startsWith("{") && arg.endsWith("}")) {
                String key = arg.substring(1, arg.length() - 1);
                File f = dataMap.get(key);
                return f != null ? f.getAbsolutePath() : arg;
            }
            if (arg.startsWith("[") && arg.endsWith("]")) {
                return mavenFile(mcDir, arg.substring(1, arg.length() - 1), null).getAbsolutePath();
            }
            return arg;
        }).toArray(String[]::new);
    }

    private static boolean outputsUpToDate(
            Map<String, String> outputs,
            Map<String, File> dataMap, File mcDir
    ) {
        if (outputs == null || outputs.isEmpty()) return false;
        for (Map.Entry<String, String> e : outputs.entrySet()) {
            String pathRaw = e.getKey();
            String expectedSha1 = e.getValue();
            File f = pathRaw.startsWith("{")
                    ? dataMap.get(pathRaw.substring(1, pathRaw.length() - 1))
                    : new File(pathRaw);
            if (f == null || !f.exists()) return false;
            if (expectedSha1 != null && !expectedSha1.equals("'") && !sha1Matches(f, expectedSha1)) return false;
        }
        return true;
    }

    static File mavenFile(File mcDir, String coords, String baseDir) {
        String[] p = coords.split(":");
        String group = p[0].replace('.', '/');
        String artifact = p[1];
        String version = p[2];
        String classifier = p.length > 3 ? p[3] : null;
        String ext = "jar";
        if (classifier != null && classifier.contains("@")) {
            ext = classifier.substring(classifier.indexOf('@') + 1);
            classifier = classifier.substring(0, classifier.indexOf('@'));
        }
        String fileName = artifact + "-" + version + (classifier != null ? "-" + classifier : "") + "." + ext;
        File base = baseDir != null ? new File(baseDir) : new File(mcDir, "libraries");
        return new File(base, group + "/" + artifact + "/" + version + "/" + fileName);
    }

    static String mavenUrl(String repoBase, String coords, String ignored) {
        String[] p = coords.split(":");
        String group = p[0].replace('.', '/');
        String artifact = p[1];
        String version = p[2];
        String classifier = p.length > 3 ? p[3] : null;
        String ext = "jar";
        if (classifier != null && classifier.contains("@")) {
            ext = classifier.substring(classifier.indexOf('@') + 1);
            classifier = classifier.substring(0, classifier.indexOf('@'));
        }
        String fileName = artifact + "-" + version + (classifier != null ? "-" + classifier : "") + "." + ext;
        return repoBase + group + "/" + artifact + "/" + version + "/" + fileName;
    }

    static void downloadFile(String url, File dest, String expectedSha1) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("User-Agent", "CotSL-Launcher/1.0");
        conn.connect();
        if (conn.getResponseCode() != 200)
            throw new IOException("HTTP " + conn.getResponseCode() + " for " + url);
        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        if (expectedSha1 != null && !sha1Matches(dest, expectedSha1))
            throw new IOException("SHA-1 mismatch for " + dest.getName() + " (expected " + expectedSha1 + ")");
    }

    static boolean sha1Matches(File file, String expected) {
        if (expected == null) return true;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] bytes = Files.readAllBytes(file.toPath());
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString().equalsIgnoreCase(expected);
        } catch (Exception e) { return false; }
    }
}