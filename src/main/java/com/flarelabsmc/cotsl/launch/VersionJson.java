package com.flarelabsmc.cotsl.launch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VersionJson {
    public String id;
    public String inheritsFrom;
    public String mainClass;
    public String assets;
    public AssetIndex assetIndex;
    public List<Library> libraries = new ArrayList<>();
    public Arguments arguments;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AssetIndex {
        public String id;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Arguments {
        public List<JsonNode> game = new ArrayList<>();
        public List<JsonNode> jvm = new ArrayList<>();
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

