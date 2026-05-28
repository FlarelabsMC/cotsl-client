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

import static com.flarelabsmc.cotsl.launch.Launcher.*;

public class InstallManager {
    public static final String DIV = "InstallManager";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String NEO_MAVEN = "https://maven.neoforged.net/releases/";
    private static final String CENTRAL = "https://repo1.maven.org/maven2/";

    public static void runInstallIfNeeded() throws Exception {
        InstallState.Options state = InstallState.get();
        logWith("Resolving Minecraft directory...", DIV);
        File mcDir = Paths.resolveMcDir(state);
        logWith("Using minecraft directory at " + mcDir, DIV);

        String reqNeoVer = getReqNeoVer();
        String reqSelfVer = getReqSelfVer();
        logWith("Required versions: NeoForge=" + reqNeoVer + ",  self=" + reqSelfVer, DIV);
        logWith("Installed versions: NeoForge=" + state.inNeoVer + ",  self=" + state.inSelfVer, DIV);

        VersionJson neoVersionJson = NeoForgeInstaller.getNeoVersionJson(reqNeoVer, mcDir);
        File neoInstaller = null;
        if (neoVersionJson == null) {
            logWith("Could not find NeoForge version.json, redownloading...", DIV);
            neoInstaller = NeoForgeInstaller.downloadInstaller(reqNeoVer);

            try (JarFile jarFile = new JarFile(neoInstaller)) {
                InstallProfile profile = MAPPER.readValue(
                        jarFile.getInputStream(jarFile.getEntry("install_profile.json")),
                        InstallProfile.class
                );

                NeoForgeInstaller.placeNeoVersionJson(jarFile, profile.json, reqNeoVer, mcDir);

                neoVersionJson = NeoForgeInstaller.getNeoVersionJson(reqNeoVer, mcDir);
                if (neoVersionJson == null)
                    throw new RuntimeException("Failed to read version.json from NeoForge installer");
            }
        }

        String mcVersion = neoVersionJson.inheritsFrom;
        logWith("Detected Minecraft version " + mcVersion, DIV);

        VersionJson vanillaVersionJson = getVanillaVersionJson(mcDir, mcVersion);
        if (vanillaVersionJson == null) {
            logWith("Could not find vanilla version.json, redownloading...", DIV);
            MCVersionManifest.Entry manifestEntry = MCVersionManifest.getManifest().getVersion(neoVersionJson.inheritsFrom);

            vanillaVersionJson = manifestEntry.requestVersionJson();
            File savedFile = Paths.getVanillaVersionJsonPath(mcDir, mcVersion);
            savedFile.getParentFile().mkdirs();
            MAPPER.writer()
                    .writeValues(savedFile)
                    .write(vanillaVersionJson)
                    .close();
        }

        logWith("Verifying installation...", DIV);
        VersionJson mergedVersionJson = vanillaVersionJson.mergeWith(neoVersionJson);

        logWith("Verifying libraries...", DIV);
        downloadLibraries(mcDir, mergedVersionJson.libraries);

        logWith("Verifying assets...", DIV);
        MCAssetIndex assetIndex = getAssetIndex(mcDir, vanillaVersionJson.assetIndex.id);
        if (assetIndex == null) {
            logWith("Could not find asset index json, redownloading...", DIV);
            assetIndex = vanillaVersionJson.requestAssetIndex();

            File savedFile = Paths.getAssetIndexFile(mcDir, vanillaVersionJson.assetIndex.id).toFile();
            savedFile.getParentFile().mkdirs();
            MAPPER.writer()
                    .writeValues(savedFile)
                    .write(assetIndex)
                    .close();
        }

        assetIndex.downloadAssets(mcDir);

        logWith("Verifying NeoForge installation...", DIV);
        NeoForgeInstaller.verifyInstallation(neoInstaller, reqNeoVer, mcDir, vanillaVersionJson);

        logWith("Finished install check", DIV);

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
                    logWith("Downloading library from artifact: " + artifact.url, DIV);
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
                logWith("Downloading library from maven: " + url, DIV);
                try { Networking.downloadFile(URI.create(url), target.toPath()); }
                catch (IOException e) {
                    url = CENTRAL + mavenPath;
                    logWith("NeoForge maven failed, trying: " + url, DIV);
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
            logErrWith("Failed to read asset index json: " + e.getMessage(), DIV, e);
            return null;
        }
    }

    public static VersionJson getVanillaVersionJson(File mcDir, String mcVersion) {
        File expectedPath = Paths.getVanillaVersionJsonPath(mcDir, mcVersion);

        if (!expectedPath.exists()) return null;

        try {
            return MAPPER.readValue(expectedPath, VersionJson.class);
        } catch (Exception e) {
            logErrWith("Failed to read vanilla version.json: " + e.getMessage(), DIV, e);
            return null;
        }
    }

    public static String getReqNeoVer() throws Exception {
        try (var is = Launcher.class.getResourceAsStream("/neo_version.txt")) {
            if (is == null) throw new FileNotFoundException("neo_version.txt not found in JAR");
            return new String(is.readAllBytes()).trim();
        }
    }

    public static String getReqSelfVer() throws Exception {
        try (var is = Launcher.class.getResourceAsStream("/cotsl_version.txt")) {
            if (is == null) throw new FileNotFoundException("cotsl_version.txt not found in JAR");
            return new String(is.readAllBytes()).trim();
        }
    }
}
