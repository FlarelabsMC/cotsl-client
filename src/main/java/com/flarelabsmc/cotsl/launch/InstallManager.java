package com.flarelabsmc.cotsl.launch;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.jar.JarFile;

import static com.flarelabsmc.cotsl.launch.LaunchAgent.*;

public class InstallManager {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String NEO_MAVEN = "https://maven.neoforged.net/releases/";
    private static final String CENTRAL = "https://repo1.maven.org/maven2/";

    public static void runInstallIfNeeded() throws Exception {
        InstallState.Options state = InstallState.get();
        log("[CotSL-Installer] Resolving Minecraft directory...");
        File mcDir = Paths.resolveMcDir(state);
        log("[CotSL-Installer] Using minecraft directory at " + mcDir);
        if (mcDir == null) {
            logErr("[CotSL] Could not find Minecraft directory. Aborting install.");
            System.exit(1);
            return;
        }
        String reqNeoVer = getReqNeoVer();
        String reqSelfVer = getReqSelfVer();
        boolean selfNeedsUpdate = !reqSelfVer.equals(state.inSelfVer);
        log("[CotSL-Installer] Required versions: NeoForge=" + reqNeoVer + ",  self=" + reqSelfVer);
        log("[CotSL-Installer] Installed versions: NeoForge=" + state.inNeoVer + ",  self=" + state.inSelfVer);

        if (selfNeedsUpdate) deploySelf(mcDir);

        VersionJson neoVersionJson = NeoForgeInstaller.getNeoVersionJson(reqNeoVer, mcDir);
        File neoInstaller = null;
        if (neoVersionJson == null) {
            log("[CotSL-Installer] Could not find NeoForge version.json, redownloading...");
            neoInstaller = NeoForgeInstaller.downloadInstaller(reqNeoVer);

            try (JarFile jarFile = new JarFile(neoInstaller)) {
                InstallProfile profile = MAPPER.readValue(
                        jarFile.getInputStream(jarFile.getEntry("install_profile.json")),
                        InstallProfile.class
                );

                NeoForgeInstaller.placeNeoVersionJson(jarFile, profile.json, reqNeoVer, mcDir);

                neoVersionJson = NeoForgeInstaller.getNeoVersionJson(reqNeoVer, mcDir);
                if (neoVersionJson == null) throw new RuntimeException("Failed to read version.json from NeoForge installer");
            }
        }

        String mcVersion = neoVersionJson.inheritsFrom;
        log("[CotSL-Installer] Detected Minecraft version " + mcVersion);

        VersionJson vanillaVersionJson = getVanillaVersionJson(mcDir, mcVersion);
        if (vanillaVersionJson == null) {
            log("[CotSL-Installer] Could not find vanilla version.json, redownloading...");
            MCVersionManifest.Entry manifestEntry = MCVersionManifest.getManifest().getVersion(neoVersionJson.inheritsFrom);

            vanillaVersionJson = manifestEntry.requestVersionJson();
            File savedFile = Paths.getVanillaVersionJsonPath(mcDir, mcVersion);
            savedFile.getParentFile().mkdirs();
            MAPPER.writer()
                    .writeValues(savedFile)
                    .write(vanillaVersionJson)
                    .close();
        }

        log("[CotSL-Installer] Verifying installation...");
        VersionJson mergedVersionJson = vanillaVersionJson.mergeWith(neoVersionJson);

        log("[CotSL-Installer] Verifying libraries...");
        downloadLibraries(mcDir, mergedVersionJson.libraries);

        log("[CotSL-Installer] Verifying assets...");
        MCAssetIndex assetIndex = getAssetIndex(mcDir, vanillaVersionJson.assetIndex.id);
        if (assetIndex == null) {
            log("[CotSL-Installer] Could not find asset index json, redownloading...");
            assetIndex = vanillaVersionJson.requestAssetIndex();

            File savedFile = Paths.getAssetIndexFile(mcDir, vanillaVersionJson.assetIndex.id).toFile();
            savedFile.getParentFile().mkdirs();
            MAPPER.writer()
                    .writeValues(savedFile)
                    .write(assetIndex)
                    .close();
        }

        assetIndex.downloadAssets(mcDir);

        log("[CotSL-Installer] Verifying NeoForge installation...");
        NeoForgeInstaller.verifyInstallation(neoInstaller, reqNeoVer, mcDir, vanillaVersionJson);

        log("[CotSL-Installer] Finished install check");

        state.mcDir = mcDir.toString();
        state.inNeoVer = reqNeoVer;
        state.inSelfVer = reqSelfVer;
        state.save();
    }

    public static void downloadLibraries(File mcDir, List<VersionJson.Library> libraries) throws IOException, InterruptedException {
        for (VersionJson.Library lib : libraries) {
            if (!MinecraftLauncher.appliesToOs(lib.rules)) continue;

            // TODO: natives extraction?
            if (lib.downloads != null && lib.downloads.artifact != null) {
                VersionJson.Library.Downloads.Artifact artifact = lib.downloads.artifact;
                File path = Paths.resolveLibraryPath(mcDir, artifact.path);

                if (path.exists() && path.length() > 0 && Paths.sha1Matches(path, artifact.sha1)) {
                    continue;
                } else {
                    log("[CotSL-Installer] Downloading library from artifact: " + artifact.url);
                    path.getParentFile().mkdirs();
                    Networking.downloadFile(URI.create(artifact.url), path.toPath());
                }
            }
            if (lib.name != null) {
                File target = Paths.mavenIdentToFile(Paths.getLibraryDir(mcDir), lib.name);
                if (target.exists() && target.length() > 0) continue;
                target.getParentFile().mkdirs();

                String mavenPath = Paths.mavenIdentToPath(lib.name);

                String url = NEO_MAVEN + mavenPath;
                log("[CotSL-Installer] Downloading library from maven: " + url);
                try { Networking.downloadFile(URI.create(url), target.toPath()); }
                catch (IOException e) {
                    url = CENTRAL + mavenPath;
                    log("[CotSL-Installer] NeoForge maven failed, trying: " + url);
                    Networking.downloadFile(URI.create(url), target.toPath());
                }
            }
        }
    }

    public static MCAssetIndex getAssetIndex(File mcDir, String assetIndexId) {
        Path expectedPath = Paths.getAssetIndexFile(mcDir, assetIndexId);

        if (!expectedPath.toFile().exists()) return null;

        try {
            return MAPPER.readValue(expectedPath.toFile(), MCAssetIndex.class);
        } catch (Exception e) {
            log("[CotSL-Installer] Failed to read asset indox json: " + e);
            return null;
        }
    }

    public static VersionJson getVanillaVersionJson(File mcDir, String mcVersion) {
        File expectedPath = Paths.getVanillaVersionJsonPath(mcDir, mcVersion);

        if (!expectedPath.exists()) return null;

        try {
            return MAPPER.readValue(expectedPath, VersionJson.class);
        } catch (Exception e) {
            log("[CotSL-Installer] Failed to read vanilla version.json: " + e);
            return null;
        }
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
