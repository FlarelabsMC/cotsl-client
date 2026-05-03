package com.flarelabsmc.cotsl.launch;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;

public class Paths {
    // In the cotsl directory
    private static final String INSTALL_STATE_FILE = "install_state.json";
    private static final String AUTH_STATE_FILE = "auth_state.json";


    public static File getInstallDir() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String home = System.getProperty("user.home", ".");
        File dir;
        if (os.contains("win") && System.getenv("APPDATA") != null) dir = new File(System.getenv("APPDATA"), ".cotsl");
        else if (os.contains("mac")) dir = new File(home, "Library/Application Support/.cotsl");
        else dir = new File(home, ".cotsl");
        dir.mkdirs();
        return dir;
    }

    public static File getInstallStatePath() {
        return new File(getInstallDir(), INSTALL_STATE_FILE);
    }
    public static File getAuthStatePath() {
        return new File(getInstallDir(), AUTH_STATE_FILE);
    }

    public static File getInstanceDir() {
        return new File(getInstallDir(), "instance");
    }

    // In the Minecraft install directory
    private static final String VERSION_DIRECTORY = "versions";
    private static final String LIBRARY_DIRECTORY = "libraries";
    private static final String ASSET_DIRECTORY = "assets";

    public static File getVersionDir(File mcDir) {
        return new File(mcDir, VERSION_DIRECTORY);
    }
    public static File getLibraryDir(File mcDir) {
        return new File(mcDir, LIBRARY_DIRECTORY);
    }
    public static File getAssetsDir(File mcDir) {
        return new File(mcDir, ASSET_DIRECTORY);
    }

    public static Path getAssetIndexFile(File mcDir, String assetIndexId) {
        return Paths.getAssetsDir(mcDir).toPath()
                .resolve("indexes")
                .resolve(assetIndexId + ".json");
    }

    /// Returns the vanilla version.json file, for example `<mcdir>/versions/26.1/26.1.json`
    public static File getVanillaVersionJsonPath(File mcDir, String mcVersion) {
        return new File(
                new File(getVersionDir(mcDir), mcVersion),
                mcVersion + ".json"
        );
    }

    /// Returns the NeoForge version.json file, for example `<mcdir>/versions/neoforge-26.1.2.19-beta/neoforge-26.1.2.19-beta.json`
    public static File getNeoVersionJsonPath(File mcDir, String neoVersion) {
        return new File(
                new File(getVersionDir(mcDir), "neoforge-" + neoVersion),
                "neoforge-" + neoVersion + ".json"
        );
    }

    /// Returns the absolute path of the library file
    /// TODO: Test on Windows
    public static File resolveLibraryPath(File mcDir, String libraryPath) {
        return new File(getLibraryDir(mcDir), libraryPath);
    }


    /// Returns the default Minecraft install directory across platforms,
    /// creating it if it does not exist
    public static File resolveMcDir(InstallState.Options state) {
        if (state.mcDir != null) {
            File saved = new File(state.mcDir);
            if (saved.isDirectory()) return saved;
        }
        File detected = getMcDir();
        if (detected.isDirectory() && launcherProfilesExist(detected)) return detected;
        System.setProperty("cotsl.install.needsMcDir", "true");
        // TODO: Implement directory picker
        String chosen = System.getProperty("cotsl.install.chosenMcDir");
        if (chosen != null) return new File(chosen);
        detected.mkdirs();
        return detected;
    }

    /// Returns the default Minecraft install directory across platforms
    ///
    /// Windows: `%APPDATA%/.minecraft/`
    ///
    /// macOS: `~/Library/Application Support/minecraft`
    ///
    /// Linux: `~/.minecraft` or with flatpak: `~/.var/app/com.mojang.Minecraft/.minecraft`
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
            if (flatpak.exists()) return flatpak;
        }
        return c;
    }

    private static boolean launcherProfilesExist(File mcDir) {
        return new File(mcDir, "launcher_profiles.json").exists() || new File(mcDir, "launcher_profiles_microsoft_store.json").exists();
    }

    static File mavenIdentToFile(File baseDir, String ident) {
        return new File(baseDir, mavenIdentToPath(ident));
    }

    static String mavenIdentToPath(String ident) {
        String[] p = ident.split(":");
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
        return group + "/" + artifact + "/" + version + "/" + fileName;
    }

    static boolean sha1Matches(File file, String expected) {
        if (expected == null) return true;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] bytes = Files.readAllBytes(file.toPath());
            byte[] digest = md.digest(bytes);

            return Arrays.equals(digest, HexFormat.of().parseHex(expected));
        } catch (Exception e) { return false; }
    }
}
