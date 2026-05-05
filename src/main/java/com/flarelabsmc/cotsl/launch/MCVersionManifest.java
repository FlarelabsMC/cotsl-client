package com.flarelabsmc.cotsl.launch;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpResponse;
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


        public VersionJson requestVersionJson() throws IOException, InterruptedException {
            HttpResponse<InputStream> response = Networking.requestStream(URI.create(this.url));

            return MAPPER.readValue(response.body(), VersionJson.class);
        }
    }

    public static MCVersionManifest getManifest() throws IOException, InterruptedException {
        HttpResponse<InputStream> response = Networking.requestStream(MANIFEST_URL);

        if (response.statusCode() != 200)
            throw new IOException("HTTP " + response.statusCode() + " for " + MANIFEST_URL);

        return MAPPER.readValue(response.body(), MCVersionManifest.class);
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

