package com.flarelabsmc.cotsl.launch;

import java.io.File;

import static com.flarelabsmc.cotsl.launch.LaunchAgent.*;

public class Paths {
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
}
