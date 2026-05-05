package com.flarelabsmc.cotsl.launch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

import static com.flarelabsmc.cotsl.launch.LaunchAgent.log;

public class InstallState {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static InstallState.Options INSTANCE = null;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Options {
        public String inNeoVer;
        public String inSelfVer;
        public String mcDir;
        public String xuid;

        public void save() {
            File path = Paths.getInstallStatePath();
            try {
                MAPPER.writerWithDefaultPrettyPrinter().writeValue(path, this);
            } catch (IOException e) {
                System.err.println("[CotSL] Could not save install state to " + path + ": " + e.getMessage());
            }
        }
    }

    public static InstallState.Options get() {
        if (INSTANCE == null) {
            INSTANCE = loadFromPath(Paths.getInstallStatePath());
        }
        return INSTANCE;
    }

    private static InstallState.Options loadFromPath(File file) {
        if (file.exists()) {
            log("[CotSL] Loading install state...");
            try {
                return MAPPER.readValue(file, InstallState.Options.class);
            } catch (Exception e) {
                System.err.println("[CotSL] Could not read install state. Starting again: " + e.getMessage());
            }
        }
        log("[CotSL] Install state file nonexistent, creating new state...");
        return new InstallState.Options();
    }
}
