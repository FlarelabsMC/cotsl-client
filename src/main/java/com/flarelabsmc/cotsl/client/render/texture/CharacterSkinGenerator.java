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
    static final int pantsTypes = 2, shoesTypes = 2;
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

    private static NativeImage getShirtOverlay(int gender, int bodyType) {
        if (bodyType < 0 || bodyType >= CharacterSkinGenerator.bodyTypes) {
            throw new IllegalArgumentException("Invalid body type index: " + bodyType);
        }
        String g = gender == 0 ? "male" : "female";
        NativeImage full = getTexture(Identifier.parse("cotsl:textures/skin/" + g + "_body_types.png"));
        NativeImage snip = Frankenstein.snip(full, 52, bodyType * 32 + 16, 24, 16);
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

    private static NativeImage getSleeveOverlay(int gender, int bodyType, boolean left) {
        if (bodyType < 0 || bodyType >= CharacterSkinGenerator.bodyTypes) {
            throw new IllegalArgumentException("Invalid body type index: " + bodyType);
        }
        String g = gender == 0 ? "male" : "female";
        NativeImage full = getTexture(Identifier.parse("cotsl:textures/skin/" + g + "_body_types.png"));
        int u = left ? 76 : 90;
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

    private static NativeImage getShoes(int type, boolean left) {
        if (type < 0 || type >= CharacterSkinGenerator.shoesTypes) {
            throw new IllegalArgumentException("Invalid shoes type index: " + type);
        }
        NativeImage full = getTexture(Identifier.parse("cotsl:textures/skin/shoes.png"));
        int u = (left ? 0 : 16) + (type * 32);
        NativeImage snip = Frankenstein.snip(full, u, 0, 16, 16);
        full.close();
        return snip;
    }

    private static NativeImage createSkin(int gender, int headShape, int jawShape, int hairType, int facialHairType, int bodyType, int legType, int shoesType, int skinColor, int eyeColor, int shirtColor, int pantsColor) {
        Frankenstein.Monster builder = new Frankenstein.Monster(64, 64);
        NativeImage headTexture = getHeadShape(headShape, skinColor);
        NativeImage jawTexture = getJawShape(jawShape, skinColor);
        NativeImage eyesTexture = getEyes(eyeColor);
        NativeImage bodyTexture = getBodyType(gender, bodyType, skinColor);
        NativeImage shirtTexture = getShirt(gender, bodyType);
        NativeImage leftSleeveTexture = getSleeve(gender, bodyType, true);
        NativeImage rightSleeveTexture = getSleeve(gender, bodyType, false);
        NativeImage leftArmTexture = getArms(skinColor, true);
        NativeImage rightArmTexture = getArms(skinColor, false);
        NativeImage leftSleeveOverlayTexture = getSleeveOverlay(gender, bodyType, true);
        NativeImage rightSleeveOverlayTexture = getSleeveOverlay(gender, bodyType, false);
        NativeImage shirtOverlayTexture = getShirtOverlay(gender, bodyType);
        NativeImage leftLegTexture = getLegs(skinColor, true);
        NativeImage rightLegTexture = getLegs(skinColor, false);
        NativeImage leftPantsTexture = getPants(legType, true);
        NativeImage rightPantsTexture = getPants(legType, false);
        builder.addTexture(headTexture, new Frankenstein.UVLocation(0, 0, 32, 16, 0, false));
        builder.addTexture(jawTexture, new Frankenstein.UVLocation(0, 0, 32, 16, 1, true));
        builder.addTexture(eyesTexture, new Frankenstein.UVLocation(0, 0, 8, 8, 0, false));
        builder.addTexture(bodyTexture, new Frankenstein.UVLocation(16, 16, 24, 16, 0, false));
        builder.addTexture(leftArmTexture, new Frankenstein.UVLocation(32, 48, 14, 16, 0, false));
        builder.addTexture(rightArmTexture, new Frankenstein.UVLocation(40, 16, 14, 16, 0, false));
        builder.addTexture(Frankenstein.tint(shirtTexture, shirtColor), new Frankenstein.UVLocation(16, 16, 24, 16, 1, true));
        builder.addTexture(Frankenstein.tint(leftSleeveTexture, shirtColor), new Frankenstein.UVLocation(32, 48, 14, 16, 1, false));
        builder.addTexture(Frankenstein.tint(rightSleeveTexture, shirtColor), new Frankenstein.UVLocation(40, 16, 14, 16, 1, false));
        builder.addTexture(Frankenstein.tint(leftSleeveOverlayTexture, shirtColor), new Frankenstein.UVLocation(48, 48, 14, 16, 1, true));
        builder.addTexture(Frankenstein.tint(rightSleeveOverlayTexture, shirtColor), new Frankenstein.UVLocation(40, 32, 14, 16, 1, true));
        builder.addTexture(Frankenstein.tint(shirtOverlayTexture, shirtColor), new Frankenstein.UVLocation(16, 32, 24, 16, 1, true));
        builder.addTexture(leftLegTexture, new Frankenstein.UVLocation(16, 48, 16, 16, 0, false));
        builder.addTexture(rightLegTexture, new Frankenstein.UVLocation(0, 16, 16, 16, 0, false));
        builder.addTexture(Frankenstein.tint(leftPantsTexture, pantsColor), new Frankenstein.UVLocation(16, 48, 16, 16, 1, false));
        builder.addTexture(Frankenstein.tint(rightPantsTexture, pantsColor), new Frankenstein.UVLocation(0, 16, 16, 16, 1, false));
        builder.addTexture(getShoes(shoesType, true), new Frankenstein.UVLocation(0, 48, 16, 16, 1, false));
        builder.addTexture(getShoes(shoesType, false), new Frankenstein.UVLocation(0, 32, 16, 16, 1, false));
        NativeImage result = builder.build();
        headTexture.close();
        jawTexture.close();
        eyesTexture.close();
        bodyTexture.close();
        shirtTexture.close();
        leftSleeveTexture.close();
        rightSleeveTexture.close();
        leftArmTexture.close();
        rightArmTexture.close();
        leftLegTexture.close();
        rightLegTexture.close();
        leftPantsTexture.close();
        rightPantsTexture.close();
        return result;
    }

    public static NativeImage createSkin(CharData data) {
        return createSkin(data.gender(), data.headShape(), data.jawShape(), data.hair(), data.facialHair(), data.bodyType(), data.legType(), data.shoesType(), data.skinColor(), data.eyesColor(), data.shirtColor(), data.pantsColor());
    }
}
