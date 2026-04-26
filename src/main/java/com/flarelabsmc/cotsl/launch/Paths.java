package com.flarelabsmc.cotsl.launch;

import java.io.File;

public class Paths {
    private static final String INSTALL_STATE_FILE = "install_state.json";

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

    public static File getInstanceDir() {
        return new File(getInstallDir(), "instance");
    }
}
