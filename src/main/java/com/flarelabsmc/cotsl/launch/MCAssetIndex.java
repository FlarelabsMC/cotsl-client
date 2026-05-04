package com.flarelabsmc.cotsl.launch;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

import static com.flarelabsmc.cotsl.launch.LaunchAgent.log;

public class MCAssetIndex {
    public Map<String, AssetObject> objects;

    public static class AssetObject {
        public String hash;
        public int size;
    }

    public void downloadAssets(File mcDir) {
        log("[CotSL-Assets] Beginning parallel asset downloading...");
        this.objects.entrySet().parallelStream().forEach(object -> {
            AssetObject asset = object.getValue();

            String prefix = asset.hash.substring(0, 2);
            String name = asset.hash;

            Path filePath = Paths.getAssetsDir(mcDir).toPath().resolve("objects").resolve(prefix).resolve(name);

            if (!filePath.toFile().exists() || filePath.toFile().length() != asset.size) {
                filePath.getParent().toFile().mkdirs();
                String url = String.format("https://resources.download.minecraft.net/%s/%s", prefix, name);
                // log("[CotSL-Assets] Downloading asset: " + url);

                try {
                    Networking.downloadFile(URI.create(url), filePath);
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        log("[CotSL-Assets] Asset downloading complete");
    }
}
