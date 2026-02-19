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
    static final int bodyTypes = 3, legTypes = 2;
    static final int pantsTypes = 2, shoesTypes = 4;
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

    private static NativeImage getBodyType(int gender, int bodyType, int skinColor) throws RuntimeException {
        if (bodyType < 0 || bodyType >= CharacterSkinGenerator.bodyTypes) {
            throw new IllegalArgumentException("Invalid body type index: " + bodyType);
        }
        String g = gender == 0 ? "male" : "female";
        NativeImage fullb = getTexture(Identifier.parse("cotsl:textures/skin/" + g + "_body_types.png"));
        NativeImage snipb = Frankenstein.snip(fullb, 24 + skinColor * 32, bodyType * 32, 24, 16);
        fullb.close();
        return snipb;
    }

    private static NativeImage getShirt(int gender, int bodyType) {
        if (bodyType < 0 || bodyType >= CharacterSkinGenerator.bodyTypes) {
            throw new IllegalArgumentException("Invalid body type index: " + bodyType);
        }
        String g = gender == 0 ? "male" : "female";
        NativeImage full = getTexture(Identifier.parse("cotsl:textures/skin/" + g + "_body_types.png"));
        NativeImage snip = Frankenstein.snip(full, 0, bodyType * 32 + 16, 24, 16);
        full.close();
        return snip;
    }

    private static NativeImage getSleeve(int gender, int bodyType, boolean left) {
        if (bodyType < 0 || bodyType >= CharacterSkinGenerator.bodyTypes) {
            throw new IllegalArgumentException("Invalid body type index: " + bodyType);
        }
        String g = gender == 0 ? "male" : "female";
        NativeImage full = getTexture(Identifier.parse("cotsl:textures/skin/" + g + "_body_types.png"));
        int u = left ? 24 : 38;
        NativeImage snip = Frankenstein.snip(full, u, bodyType * 32 + 16, 14, 16);
        full.close();
        return snip;
    }

    private static NativeImage getArms(int skinColor, boolean left) {
        if (skinColor < 0 || skinColor >= CharacterSkinGenerator.skinColors) {
            throw new IllegalArgumentException("Invalid skin color index: " + skinColor);
        }
        NativeImage full = getTexture(Identifier.parse("cotsl:textures/skin/arm_types.png"));
        int u = 32 + skinColor * 32;
        int v = left ? 0 : 16;
        NativeImage snip = Frankenstein.snip(full, u, v, 14, 16);
        full.close();
        return snip;
    }

    private static NativeImage getLegs(int skinColors, boolean left) {
        if (skinColors < 0 || skinColors >= CharacterSkinGenerator.skinColors) {
            throw new IllegalArgumentException("Invalid skin color index: " + skinColors);
        }
        NativeImage full = getTexture(Identifier.parse("cotsl:textures/skin/leg_types.png"));
        int u = (left ? 48 : 32) + skinColors * 32;
        NativeImage snip = Frankenstein.snip(full, u, 0, 16, 16);
        full.close();
        return snip;
    }

    private static NativeImage getPants(int type, boolean left) {
        if (type < 0 || type >= CharacterSkinGenerator.pantsTypes) {
            throw new IllegalArgumentException("Invalid pants type index: " + type);
        }
        NativeImage full = getTexture(Identifier.parse("cotsl:textures/skin/leg_types.png"));
        int u = (left ? 0 : 16) + (type * 32);
        NativeImage snip = Frankenstein.snip(full, u, 32, 16, 16);
        full.close();
        return snip;
    }

    private static NativeImage createSkin(int gender, int headShape, int jawShape, int hairType, int facialHairType, int bodyType, int legType, int shoesType, int skinColor, int eyeColor) {
        Frankenstein.Monster builder = new Frankenstein.Monster(64, 64);
        NativeImage headTexture = getHeadShape(headShape, skinColor);
        NativeImage jawTexture = getJawShape(jawShape, skinColor);
        NativeImage eyesTexture = getEyes(eyeColor);
        builder.addTexture(headTexture, new Frankenstein.UVLocation(0, 0, 32, 16, 0, false));
        builder.addTexture(jawTexture, new Frankenstein.UVLocation(0, 0, 32, 16, 1, true));
        builder.addTexture(eyesTexture, new Frankenstein.UVLocation(0, 0, 8, 8, 0, false));
        builder.addTexture(getBodyType(gender, bodyType, skinColor), new Frankenstein.UVLocation(16, 16, 24, 16, 0, false));
        builder.addTexture(getShirt(gender, bodyType), new Frankenstein.UVLocation(16, 32, 24, 16, 0, false));
        builder.addTexture(getSleeve(gender, bodyType, true), new Frankenstein.UVLocation(40, 32, 14, 16, 0, false));
        builder.addTexture(getSleeve(gender, bodyType, false), new Frankenstein.UVLocation(48, 48, 14, 16, 0, false));
        builder.addTexture(getArms(skinColor, true), new Frankenstein.UVLocation(32, 48, 14, 16, 0, false));
        builder.addTexture(getArms(skinColor, false), new Frankenstein.UVLocation(40, 16, 14, 16, 0, false));
        builder.addTexture(getLegs(skinColor, true), new Frankenstein.UVLocation(16, 48, 16, 16, 0, false));
        builder.addTexture(getLegs(skinColor, false), new Frankenstein.UVLocation(0, 16, 16, 16, 0, false));
        builder.addTexture(getPants(legType, true), new Frankenstein.UVLocation(0, 48, 16, 16, 0, false));
        builder.addTexture(getPants(legType, false), new Frankenstein.UVLocation(16, 48, 16, 16, 0, false));
        NativeImage result = builder.build();
        headTexture.close();
        jawTexture.close();
        eyesTexture.close();
        return result;
    }

    public static NativeImage createSkin(CharData data) {
        return createSkin(data.gender(), data.headShape(), data.jawShape(), data.hair(), data.facialHair(), data.bodyType(), data.legType(), data.shoesType(), data.skinColor(), data.eyesColor());
    }
}
