package com.flarelabsmc.cotsl.launch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class InstallState {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static InstallState.Options INSTANCE = null;
    private static final String INSTALL_STATE_FILE = "install_state.json";

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Options {
        public String inNeoVer;
        public String inSelfVer;
        public String mcDir;
        public String authToken;
        public String playerName;
        public String playerUuid;
        public String xuid;
        public long authExpiry;

        public void save() {
            File path = getPath();
            try {
                MAPPER.writerWithDefaultPrettyPrinter().writeValue(path, this);
            } catch (IOException e) {
                System.err.println("[CotSL] Could not save install state to " + path + ": " + e.getMessage());
            }
        }
    }

    public static InstallState.Options get() {
        if (INSTANCE == null) {
            INSTANCE = loadFromPath(getPath());
        }
        return INSTANCE;
    }

    public static File getPath() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String home = System.getProperty("user.home", ".");
        File dir;
        if (os.contains("win") && System.getenv("APPDATA") != null) dir = new File(System.getenv("APPDATA"), ".cotsl");
        else if (os.contains("mac")) dir = new File(home, "Library/Application Support/.cotsl");
        else dir = new File(home, ".cotsl");
        dir.mkdirs();
        return new File(dir, INSTALL_STATE_FILE);
    }

    private static InstallState.Options loadFromPath(File file) {
        if (file.exists()) {
            try {
                return MAPPER.readValue(file, InstallState.Options.class);
            } catch (Exception e) {
                System.err.println("[CotSL] Could not read install state. Starting again: " + e.getMessage());
            }
        }
        return new InstallState.Options();
    }
}
