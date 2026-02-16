package com.flarelabsmc.cotsl.common.storage.player;

import com.flarelabsmc.cotsl.common.storage.type.RecordType;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

public record CharData(
        int headShape,
        int jawShape,
        int hair,
        int hairColor,
        int facialHair,
        int bodyType,
        int shirtType,
        int shirtColor,
        int pantsType,
        int pantsColor,
        int shoesType,
        int shoesColor,
        boolean canChangeSkinColor,
        int skinColor,
        int eyesColor,
        int eyebrowsColor,
        double height
) {
    public static CharData init() {
        return new CharData(
                0, 0, 0, 0x000000, 0, 0, 0, 0xFFFFFF,
                0, 0xFFFFFF, 0, 0xFFFFFF, true,
                0xF5CFA3, 0xFFFFFF, 0xFFFFFF, 1.0
        );
    }

    public Builder rebuild() {
        return new Builder(this);
    }

    public static class Builder {
        private int headShape, jawShape, hair, hairColor, facialHair, bodyType;
        private int shirtType, shirtColor, pantsType, pantsColor, shoesType, shoesColor;
        private boolean canChangeSkinColor;
        private int skinColor, eyesColor, eyebrowsColor;
        private double height;

        public Builder(CharData data) {
            this.headShape = data.headShape;
            this.jawShape = data.jawShape;
            this.hair = data.hair;
            this.hairColor = data.hairColor;
            this.facialHair = data.facialHair;
            this.bodyType = data.bodyType;
            this.shirtType = data.shirtType;
            this.shirtColor = data.shirtColor;
            this.pantsType = data.pantsType;
            this.pantsColor = data.pantsColor;
            this.shoesType = data.shoesType;
            this.shoesColor = data.shoesColor;
            this.canChangeSkinColor = data.canChangeSkinColor;
            this.skinColor = data.skinColor;
            this.eyesColor = data.eyesColor;
            this.eyebrowsColor = data.eyebrowsColor;
            this.height = data.height;
        }

        public Builder headShape(int headShape) { this.headShape = headShape; return this; }
        public Builder jawShape(int jawShape) { this.jawShape = jawShape; return this; }
        public Builder hair(int hair) { this.hair = hair; return this; }
        public Builder hairColor(int hairColor) { this.hairColor = hairColor; return this; }
        public Builder facialHair(int facialHair) { this.facialHair = facialHair; return this; }
        public Builder bodyType(int bodyType) { this.bodyType = bodyType; return this; }
        public Builder shirtType(int shirtType) { this.shirtType = shirtType; return this; }
        public Builder shirtColor(int shirtColor) { this.shirtColor = shirtColor; return this; }
        public Builder pantsType(int pantsType) { this.pantsType = pantsType; return this; }
        public Builder pantsColor(int pantsColor) { this.pantsColor = pantsColor; return this; }
        public Builder shoesType(int shoesType) { this.shoesType = shoesType; return this; }
        public Builder shoesColor(int shoesColor) { this.shoesColor = shoesColor; return this; }
        public Builder canChangeSkinColor(boolean canChangeSkinColor) { this.canChangeSkinColor = canChangeSkinColor; return this; }
        public Builder skinColor(int skinColor) { this.skinColor = skinColor; return this; }
        public Builder eyesColor(int eyesColor) { this.eyesColor = eyesColor; return this; }
        public Builder eyebrowsColor(int eyebrowsColor) { this.eyebrowsColor = eyebrowsColor; return this; }
        public Builder height(double height) { this.height = height; return this; }

        public CharData build() {
            return new CharData(headShape, jawShape, hair, hairColor, facialHair,
                    bodyType, shirtType, shirtColor, pantsType, pantsColor,
                    shoesType, shoesColor, canChangeSkinColor, skinColor,
                    eyesColor, eyebrowsColor, height);
        }
    }

    public static final StreamCodec<ByteBuf, CharData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public @NotNull CharData decode(@NotNull ByteBuf buffer) {
            try {
                int length = ByteBufCodecs.VAR_INT.decode(buffer);
                byte[] bytes = new byte[length];
                buffer.readBytes(bytes);
                String json = new String(bytes, StandardCharsets.UTF_8);
                return (CharData) RecordType.getSingleton()
                        .parseDefaultString(null, json);
            } catch (Exception e) {
                throw new RuntimeException("Failed to decode CharData", e);
            }
        }

        @Override
        public void encode(ByteBuf buffer, CharData data) {
            try {
                String json = (String) RecordType.getSingleton()
                        .javaToSqlArg(null, data);
                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                ByteBufCodecs.VAR_INT.encode(buffer, bytes.length);
                buffer.writeBytes(bytes);
            } catch (Exception e) {
                throw new RuntimeException("Failed to encode CharData", e);
            }
        }
    };
}
