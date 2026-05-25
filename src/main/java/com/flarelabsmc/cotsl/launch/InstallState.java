package com.flarelabsmc.cotsl.launch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

import static com.flarelabsmc.cotsl.launch.Launcher.*;

public class InstallState {
    public static final String DIV = "InstallState";

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
                logErrWith("Could not save install state to " + path + ": " + e.getMessage(), DIV);
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
            logWith("Loading install state...", DIV);
            try {
                return MAPPER.readValue(file, InstallState.Options.class);
            } catch (Exception e) {
                logErrWith("Could not read install state. Starting again: " + e.getMessage(), DIV, e);
            }
        }
        logWith("Install state file nonexistent, creating new state...", DIV);
        return new InstallState.Options();
    }
}
