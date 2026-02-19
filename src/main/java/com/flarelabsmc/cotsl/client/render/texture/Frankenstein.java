package com.flarelabsmc.cotsl.client.render.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

    /**
     * Texture builder. The name is clear.
     */
    public static class Monster {
        private final int width;
        private final int height;
        private final Map<NativeImage, UVLocation> textures;

        public Monster(int width, int height) {
            this.width = width;
            this.height = height;
            this.textures = new HashMap<>();
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
            textures.put(NativeImage.read(t.get().open()), uvLocation);
            return this;
        }

        public Monster addTexture(NativeImage texture, UVLocation uvLocation) {
            if (uvLocation.u < 0 || uvLocation.v < 0 || uvLocation.width <= 0 || uvLocation.height <= 0) {
                throw new UnsupportedOperationException("[Frankenstein] Invalid UV coordinates or dimensions of texture with UVLocation " + uvLocation);
            }
            if (uvLocation.u + uvLocation.width > this.width || uvLocation.v + uvLocation.height > this.height) {
                throw new UnsupportedOperationException("[Frankenstein] Texture exceeds Monster dimensions. Texture with UVLocation " + uvLocation + ", exceeds Monster dimensions of width=" + this.width + ", height=" + this.height);
            }
            textures.put(texture, uvLocation);
            return this;
        }

        public NativeImage build() {
            NativeImage stitchedImage = new NativeImage(width, height, true);
            for (int x = 0; x < this.width; x++)
                for (int y = 0; y < this.height; y++) stitchedImage.setPixel(x, y, 0);
            Map<NativeImage, UVLocation> texturesByLayer = this.textures.entrySet().stream().sorted(Comparator.comparingInt(e -> e.getValue().layer)).collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);
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
