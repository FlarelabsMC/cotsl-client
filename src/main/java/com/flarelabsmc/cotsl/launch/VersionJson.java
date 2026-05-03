package com.flarelabsmc.cotsl.launch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VersionJson {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    public String id;
    public String inheritsFrom;
    public String mainClass;
    public String assets;
    public AssetIndex assetIndex;
    public List<Library> libraries = new ArrayList<>();
    public Arguments arguments;
    public Downloads downloads;


    /// Returns a new `VersionJson` with the merged contents of the two.
    /// `other` is expected to be the modloader version.json, `this` the vanilla one
    public VersionJson mergeWith(VersionJson other) {
        VersionJson merged = new VersionJson();

        merged.id = other.id;
        merged.inheritsFrom = other.inheritsFrom;
        merged.mainClass = other.mainClass;

        merged.assets = this.assets;
        merged.assetIndex = this.assetIndex;

        merged.libraries.addAll(this.libraries);
        merged.libraries.addAll(other.libraries);

        merged.arguments = new Arguments();
        //if (this.arguments != null && other.arguments != null) {
            merged.arguments.game.addAll(this.arguments.game);
            merged.arguments.game.addAll(other.arguments.game);

            merged.arguments.jvm.addAll(this.arguments.jvm);
            merged.arguments.jvm.addAll(other.arguments.jvm);
        //}

        return merged;
    }

    public MCAssetIndex requestAssetIndex() throws IOException, InterruptedException {
        HttpResponse<InputStream> response = Networking.requestStream(URI.create(this.assetIndex.url));

        return MAPPER.readValue(response.body(), MCAssetIndex.class);
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AssetIndex {
        public String id;
        public String sha1;
        public int size;
        public int totalSize;
        public String url;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Arguments {
        public List<JsonNode> game = new ArrayList<>();
        public List<JsonNode> jvm = new ArrayList<>();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Downloads {
        public JarDownload client;
        public JarDownload server;

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class JarDownload {
            public String sha1;
            public String url;
            public int size;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Library {
        public String name;
        public Downloads downloads;
        public List<Rule> rules;
        public Map<String, String> natives;

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Downloads {
            public Artifact artifact;

            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Artifact {
                public String path;
                public String url;
                public String sha1;
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Rule {
        public String action;
        public OsRule os;
        public Map<String, Boolean> features;

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class OsRule {
            public String name;
        }
    }
}

