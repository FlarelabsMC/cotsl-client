package com.flarelabsmc.cotsl.launch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.*;

public class MinecraftLauncher {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static Process launch(File mcDir, File gameDir, InstallState.Options state, String neoVer, File agentJar) throws Exception {
        VersionJson neo = MAPPER.readValue(
                new File(mcDir, "versions/neoforge-" + neoVer + "/neoforge-" + neoVer + ".json"),
                VersionJson.class
        );
        VersionJson vanilla = MAPPER.readValue(
                new File(mcDir, "versions/" + neo.inheritsFrom + "/" + neo.inheritsFrom + ".json"),
                VersionJson.class
        );
        List<VersionJson.Library> allLibs = new ArrayList<>(vanilla.libraries);
        allLibs.addAll(neo.libraries);
        List<String> classpath = new ArrayList<>();
        for (VersionJson.Library lib : allLibs) {
            if (!appliesToOs(lib.rules)) continue;
            File jar = resolveLibraryPath(mcDir, lib);
            if (jar != null && jar.exists()) classpath.add(jar.getAbsolutePath());
        }
        File vanillaJar = new File(mcDir, "versions/" + neo.inheritsFrom + "/" + neo.inheritsFrom + ".jar");
        if (vanillaJar.exists()) classpath.add(vanillaJar.getAbsolutePath());
        File nativesDir = new File(mcDir, "versions/" + neo.inheritsFrom + "/natives");
        nativesDir.mkdirs();
        String assetIndex = vanilla.assetIndex != null ? vanilla.assetIndex.id
                : (vanilla.assets != null ? vanilla.assets : neo.inheritsFrom);
        Map<String, String> vars = new HashMap<>();
        vars.put("natives_directory",  nativesDir.getAbsolutePath());
        vars.put("launcher_name",      "CotSL");
        vars.put("launcher_version",   "1.0.0");
        vars.put("classpath",          String.join(File.pathSeparator, classpath));
        vars.put("classpath_separator", File.pathSeparator);
        vars.put("library_directory",  new File(mcDir, "libraries").getAbsolutePath());
        vars.put("version_name",       "neoforge-" + neoVer);
        vars.put("game_directory",     gameDir.getAbsolutePath());
        vars.put("assets_root",        new File(mcDir, "assets").getAbsolutePath());
        vars.put("assets_index_name",  assetIndex);
        vars.put("auth_player_name",   state.playerName  != null ? state.playerName  : "Player");
        vars.put("auth_uuid",          state.playerUuid  != null ? state.playerUuid  : "00000000-0000-0000-0000-000000000000");
        vars.put("auth_access_token",  state.authToken   != null ? state.authToken   : "0");
        vars.put("auth_xuid",          state.xuid        != null ? state.xuid        : "0");
        vars.put("clientid",           "0");
        vars.put("user_type",          "msa");
        vars.put("version_type",       "release");
        vars.put("resolution_width",   "854");
        vars.put("resolution_height",  "480");

        String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        List<String> cmd = new ArrayList<>();
        cmd.add(java);
        if (agentJar != null && agentJar.exists()) cmd.add("-javaagent:" + agentJar.getAbsolutePath());
        cmd.add("-Dcotsl.minecraft.launch=true");

        if (vanilla.arguments != null) addArgs(cmd, vanilla.arguments.jvm, vars);
        if (neo.arguments != null) addArgs(cmd, neo.arguments.jvm, vars);
        cmd.add(neo.mainClass != null ? neo.mainClass : vanilla.mainClass);
        if (vanilla.arguments != null) addArgs(cmd, vanilla.arguments.game, vars);
        if (neo.arguments != null) addArgs(cmd, neo.arguments.game, vars);

        gameDir.mkdirs();
        new File(gameDir, "mods").mkdirs();
        return new ProcessBuilder(cmd).directory(gameDir).inheritIO().start();
    }

    private static File resolveLibraryPath(File mcDir, VersionJson.Library lib) {
        if (lib.downloads != null && lib.downloads.artifact != null && lib.downloads.artifact.path != null)
            return new File(mcDir, "libraries/" + lib.downloads.artifact.path);
        if (lib.name == null) return null;
        String[] parts = lib.name.split(":");
        if (parts.length < 3) return null;
        String group = parts[0].replace('.', '/');
        String classifier = parts.length > 3 ? "-" + parts[3] : "";
        return new File(mcDir, "libraries/" + group + "/" + parts[1] + "/" + parts[2]
                + "/" + parts[1] + "-" + parts[2] + classifier + ".jar");
    }

    private static boolean appliesToOs(List<VersionJson.Rule> rules) {
        if (rules == null || rules.isEmpty()) return true;
        String os = System.getProperty("os.name", "").toLowerCase();
        String osName = os.contains("win") ? "windows" : os.contains("mac") ? "osx" : "linux";
        boolean result = false;
        for (VersionJson.Rule rule : rules) {
            if (rule.features != null && !rule.features.isEmpty()) continue;
            boolean matches = rule.os == null || osName.equals(rule.os.name);
            if (matches) result = "allow".equals(rule.action);
        }
        return result;
    }

    private static void addArgs(List<String> cmd, List<JsonNode> args, Map<String, String> vars) {
        if (args == null) return;
        for (JsonNode node : args) {
            if (node.isTextual()) {
                cmd.add(sub(node.asText(), vars));
            } else if (node.isObject()) {
                JsonNode rulesNode = node.get("rules");
                if (rulesNode != null) {
                    try {
                        List<VersionJson.Rule> rules = MAPPER.readerForListOf(VersionJson.Rule.class).readValue(rulesNode);
                        if (!appliesToOs(rules)) continue;
                    } catch (Exception ignored) {}
                }
                JsonNode value = node.get("value");
                if (value == null) continue;
                if (value.isTextual())    cmd.add(sub(value.asText(), vars));
                else if (value.isArray()) value.forEach(v -> cmd.add(sub(v.asText(), vars)));
            }
        }
    }

    private static String sub(String s, Map<String, String> vars) {
        for (Map.Entry<String, String> e : vars.entrySet())
            if (e.getValue() != null) s = s.replace("${" + e.getKey() + "}", e.getValue());
        return s;
    }
}



