package com.flarelabsmc.cotsl.client.speech;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.client.Minecraft;

import java.io.InputStreamReader;
import java.util.*;

public class SpeechData {
    private static final Map<String, Speech> DATA = new HashMap<>();
    private static final Map<String, Integer> CUES = Map.of(
            "A", 0, "B", 1, "C", 2, "D", 3, "E", 4, "F", 5, "G", 6, "H", 7, "X", 8
    );

    public static void load() {
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager()
                    .getResource(Identifier.parse("cotsl:speech.json"));
            if (resource.isEmpty()) return;
            JsonObject root = JsonParser.parseReader(new InputStreamReader(resource.get().open())).getAsJsonObject();
            root.entrySet().forEach(entry -> {
                JsonObject data = entry.getValue().getAsJsonObject();
                List<Cue> cues = new ArrayList<>();
                data.getAsJsonArray("mouthCues").forEach(cue -> {
                    JsonObject c = cue.getAsJsonObject();
                    cues.add(new Cue(
                            c.get("start").getAsFloat(),
                            c.get("end").getAsFloat(),
                            CUES.getOrDefault(c.get("value").getAsString(), 8)
                    ));
                });
                DATA.put(entry.getKey(), new Speech(
                        data.get("sound").getAsString(),
                        data.get("duration").getAsFloat(),
                        cues
                ));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getMouthPoseAtTime(String key, float time) {
        Speech speech = DATA.get(key);
        if (speech == null) return 8;
        for (Cue cue : speech.mouthCues) {
            if (time >= cue.start && time < cue.end) {
                return cue.pose;
            }
        }
        return 8;
    }

    public static float getDuration(String key) {
        Speech speech = DATA.get(key);
        return speech != null ? speech.duration : 0;
    }

    public record Speech(String sound, float duration, List<Cue> mouthCues) {}
    public record Cue(float start, float end, int pose) {}
}