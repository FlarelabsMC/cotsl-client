package com.flarelabsmc.cotsl.launch;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MCVersionManifest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final URI MANIFEST_URL = URI.create("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json");

    public ArrayList<Entry> versions;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entry {
        public String id;
        public String type;
        public String url;
        public String sha1;
    }

    public static MCVersionManifest getManifest() throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) MANIFEST_URL.toURL().openConnection();
        connection.setRequestProperty("User-Agent", "CotSL-Launcher/1.0");
        connection.connect();

        if (connection.getResponseCode() != 200)
            throw new IOException("HTTP " + connection.getResponseCode() + " for " + MANIFEST_URL);
        try (InputStream in = connection.getInputStream()) {
            return MAPPER.readValue(in, MCVersionManifest.class);
        }
    }

    public Entry getVersion(String id) {
        for (Entry entry : this.versions) {
            if (entry.id.equals(id)) {
                return entry;
            }
        }
        return null;
    }
}

