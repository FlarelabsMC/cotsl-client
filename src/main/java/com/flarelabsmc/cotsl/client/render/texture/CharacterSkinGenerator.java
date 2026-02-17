package com.flarelabsmc.cotsl.client.render.texture;

import com.flarelabsmc.cotsl.common.storage.player.CharData;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.util.Optional;

public class CharacterSkinGenerator {
    static final int headShapes = 4, jawShapes = 4;
    static final int hairTypes = 8, facialHairTypes = 8;
    static final int bodyTypes = 8, legTypes = 2;
    static final int shoesTypes = 4;
    static final int skinColors = 5, eyeColors = 8;

    private static NativeImage getTexture(Identifier texture) throws RuntimeException {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (resource.isEmpty()) {
            throw new RuntimeException("[CharacterSkinGenerator] Texture not found: " + texture);
        }
        try {
            return NativeImage.read(resource.get().open());
        } catch (Exception e) {
            throw new RuntimeException("[CharacterSkinGenerator] Failed to read texture: " + texture, e);
        }
    }

    private static NativeImage getHeadShape(int headShape, int skinColor) throws RuntimeException {
        if (headShape < 0 || headShape >= CharacterSkinGenerator.headShapes) {
            throw new IllegalArgumentException("Invalid head shape index: " + headShape);
        }
        NativeImage full = getTexture(Identifier.parse("cotsl:textures/skin/head_shapes.png"));
        NativeImage snip = Frankenstein.snip(full, skinColor * 32, headShape * 16, 32, 16);
        full.close();
        return snip;
    }

    private static NativeImage getJawShape(int jawShape, int skinColor) throws RuntimeException {
        if (jawShape < 0 || jawShape >= CharacterSkinGenerator.jawShapes) {
            throw new IllegalArgumentException("Invalid jaw shape index: " + jawShape);
        }
        NativeImage full = getTexture(Identifier.parse("cotsl:textures/skin/head_shapes.png"));
        NativeImage snip = Frankenstein.snip(full, skinColor * 32, jawShape * 16 + 64, 32, 16);
        full.close();
        return snip;
    }

    private static NativeImage getEyes(int eyeColor) throws RuntimeException {
        if (eyeColor < 0 || eyeColor >= CharacterSkinGenerator.eyeColors) {
            throw new IllegalArgumentException("Invalid eye color index: " + eyeColor);
        }
        NativeImage full = getTexture(Identifier.parse("cotsl:textures/skin/eyes.png"));
        NativeImage snip = Frankenstein.snip(full, eyeColor * 8, 0, 8, 8);
        full.close();
        return snip;
    }

    public static NativeImage createSkin(int headShape, int jawShape, int hairType, int facialHairType, int bodyType, int legType, int shoesType, int skinColor, int eyeColor) {
        Frankenstein.Monster builder = new Frankenstein.Monster(64, 64);
        NativeImage headTexture = getHeadShape(headShape, skinColor);
        NativeImage jawTexture = getJawShape(jawShape, skinColor);
        builder.addTexture(headTexture, new Frankenstein.UVLocation(0, 0, 32, 16, 0, false));
        builder.addTexture(jawTexture, new Frankenstein.UVLocation(0, 0, 32, 16, 1, true));
        builder.addTexture(getEyes(eyeColor), new Frankenstein.UVLocation(0, 0, 8, 8, 0, false));
        NativeImage result = builder.build();
        headTexture.close();
        jawTexture.close();
        return result;
    }

    public static NativeImage createSkin(CharData data) {
        return createSkin(data.headShape(), data.jawShape(), data.hair(), data.facialHair(), data.bodyType(), data.legType(), data.shoesType(), data.skinColor(), data.eyesColor());
    }
}
