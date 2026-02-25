package com.flarelabsmc.cotsl.client.render.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.util.*;

// stitch deez nuts am i right
public class Frankenstein {
    private static final Map<Identifier, DynamicTexture> textureCache = new HashMap<>();

    public static DynamicTexture getCachedTexture(Identifier location) {
        return textureCache.get(location);
    }

    public static DynamicTexture registerTexture(Identifier location, NativeImage image) {
        DynamicTexture dynamicTexture = new DynamicTexture(location::toString, image);
        textureCache.put(location, dynamicTexture);
        Minecraft.getInstance().getTextureManager().register(location, dynamicTexture);
        return dynamicTexture;
    }

    public static NativeImage paletteSwap(Map<Integer, Integer> colorMap, NativeImage sourceImage) {
        NativeImage resultImage = new NativeImage(sourceImage.getWidth(), sourceImage.getHeight(), true);
        for (int x = 0; x < sourceImage.getWidth(); x++)
            for (int y = 0; y < sourceImage.getHeight(); y++) {
                int pixel = sourceImage.getPixel(x, y);
                int alpha = (pixel >> 24) & 0xFF;
                if (alpha == 0) {
                    resultImage.setPixel(x, y, pixel);
                    continue;
                }
                int rgb = pixel & 0x00FFFFFF;
                if (colorMap.containsKey(rgb)) {
                    int newRgb = colorMap.get(rgb);
                    int newPixel = (alpha << 24) | newRgb;
                    resultImage.setPixel(x, y, newPixel);
                } else resultImage.setPixel(x, y, pixel);
            }
        return resultImage;
    }

    public static NativeImage snip(NativeImage source, int u, int v, int width, int height) {
        NativeImage resultImage = new NativeImage(width, height, true);
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++) {
                int pixel = source.getPixel(u + x, v + y);
                resultImage.setPixel(x, y, pixel);
            }
        return resultImage;
    }

    public static NativeImage tint(NativeImage source, int tintColor) {
        NativeImage resultImage = new NativeImage(source.getWidth(), source.getHeight(), true);
        int tintR = (tintColor >> 16) & 0xFF;
        int tintG = (tintColor >> 8) & 0xFF;
        int tintB = tintColor & 0xFF;
        for (int x = 0; x < source.getWidth(); x++) {
            for (int y = 0; y < source.getHeight(); y++) {
                int pixel = source.getPixel(x, y);
                int alpha = (pixel >> 24) & 0xFF;
                if (alpha == 0) {
                    resultImage.setPixel(x, y, pixel);
                    continue;
                }
                int r = ((pixel >> 16) & 0xFF) * tintR / 255;
                int g = ((pixel >> 8) & 0xFF) * tintG / 255;
                int b = (pixel & 0xFF) * tintB / 255;
                int tinted = (alpha << 24) | (r << 16) | (g << 8) | b;
                resultImage.setPixel(x, y, tinted);
            }
        }
        return resultImage;
    }

    /**
     * Texture builder. The name is clear.
     */
    public static class Monster {
        private final int width;
        private final int height;
//        private final Map<NativeImage, UVLocation> textures;
        private final Map<Pair<NativeImage, Integer>, UVLocation> textures;
        private Pair<Integer, Integer> counter;


        public Monster(int width, int height) {
            this.width = width;
            this.height = height;
            this.textures = new HashMap<>();
            this.counter = Pair.of(0, 0);
        }

        public Monster addTexture(Identifier texture, UVLocation uvLocation) throws IOException {
            if (uvLocation.u < 0 || uvLocation.v < 0 || uvLocation.width <= 0 || uvLocation.height <= 0) {
                throw new UnsupportedOperationException("[Frankenstein] Invalid UV coordinates or dimensions of texture: \"" + texture.toString() + "\", with UVLocation " + uvLocation);
            }
            if (uvLocation.u + uvLocation.width > this.width || uvLocation.v + uvLocation.height > this.height) {
                throw new UnsupportedOperationException("[Frankenstein] Texture exceeds Monster dimensions. Texture: \"" + texture.toString() + "\", with UVLocation " + uvLocation + ", exceeds Monster dimensions of width=" + this.width + ", height=" + this.height);
            }
            Optional<Resource> t = Minecraft.getInstance().getResourceManager().getResource(texture);
            if (t.isEmpty()) throw new IOException("[Frankenstein] Texture not found: " + texture);
            textures.put(Pair.of(NativeImage.read(t.get().open()), counter.getFirst()), uvLocation);
            counter = Pair.of(counter.getFirst() + 1, uvLocation.layer);
            return this;
        }

        public Monster addTexture(NativeImage texture, UVLocation uvLocation) {
            if (uvLocation.u < 0 || uvLocation.v < 0 || uvLocation.width <= 0 || uvLocation.height <= 0) {
                throw new UnsupportedOperationException("[Frankenstein] Invalid UV coordinates or dimensions of texture with UVLocation " + uvLocation);
            }
            if (uvLocation.u + uvLocation.width > this.width || uvLocation.v + uvLocation.height > this.height) {
                throw new UnsupportedOperationException("[Frankenstein] Texture exceeds Monster dimensions. Texture with UVLocation " + uvLocation + ", exceeds Monster dimensions of width=" + this.width + ", height=" + this.height);
            }
            textures.put(Pair.of(texture, uvLocation.layer), uvLocation);
            counter = Pair.of(counter.getFirst() + 1, uvLocation.layer);
            return this;
        }

        public NativeImage build() {
            NativeImage stitchedImage = new NativeImage(width, height, true);
            for (int x = 0; x < this.width; x++)
                for (int y = 0; y < this.height; y++) stitchedImage.setPixel(x, y, 0);
//            Map<NativeImage, UVLocation> texturesByLayer = this.textures.entrySet().stream().sorted(Comparator.comparingInt(e -> e.getValue().layer)).collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);
            Map<NativeImage, UVLocation> texturesByLayer = new LinkedHashMap<>();
                this.textures.entrySet().stream()
                        .sorted(Comparator.comparingInt(e -> e.getValue().layer))
                        .forEachOrdered(e -> texturesByLayer.put(e.getKey().getFirst(), e.getValue()));
            for (Map.Entry<NativeImage, UVLocation> entry : texturesByLayer.entrySet()) {
                NativeImage texture = entry.getKey();
                UVLocation uvLocation = entry.getValue();
                for (int x = 0; x < uvLocation.width; x++)
                    for (int y = 0; y < uvLocation.height; y++) {
                        int pixel = texture.getPixel(x, y);
                        int alpha = (pixel >> 24) & 0xFF;

                        if (alpha == 0) continue;
                        if (uvLocation.mixAlpha) {
                            int existingPixel = stitchedImage.getPixel(uvLocation.u + x, uvLocation.v + y);
                            int existingAlpha = (existingPixel >> 24) & 0xFF;
                            int newAlpha = (pixel >> 24) & 0xFF;
                            if (existingAlpha > 0 && newAlpha > 0) {
                                int mixedAlpha = (existingAlpha + newAlpha) / 2;
                                pixel = (pixel & 0x00FFFFFF) | (mixedAlpha << 24);
                            }
                        }
                        stitchedImage.setPixel(uvLocation.u + x, uvLocation.v + y, pixel);
                    }
            }
            return stitchedImage;
        }
    }

    public record UVLocation(int u, int v, int width, int height, int layer, boolean mixAlpha) {}
}
