package com.flarelabsmc.cotsl.launch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InstallProfile {
    public String minecraft;
    public String json;
    public String path;
    public List<VersionJson.Library> libraries;
    public Map<String, DataValue> data;
    public List<Processor> processors;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataValue {
        public String client;
        public String server;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Processor {
        public String jar;
        public List<String> classpath;
        public List<String> args;
        public List<String> sides;
        public Map<String, String> outputs;
    }
}