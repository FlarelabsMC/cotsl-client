package com.flarelabsmc.cotsl.launch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InstallState {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    public String inNeoVer;
    public String inSelfVer;
    public String mcDir;
    public String authToken;
    public String playerName;
    public String playerUuid;
    public String xuid;
    public long authExpiry;

    public static InstallState load(File file) {
        if (file.exists()) {
            try {
                return MAPPER.readValue(file, InstallState.class);
            } catch (Exception e) {
                System.err.println("[CotSL] Could not read install state. Starting again: " + e.getMessage());
            }
        }
        return new InstallState();
    }

    public void save(File file) {
        try {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, this);
        } catch (IOException e) {
            System.err.println("[CotSL] Could not save install state: " + e.getMessage());
        }
    }
}
