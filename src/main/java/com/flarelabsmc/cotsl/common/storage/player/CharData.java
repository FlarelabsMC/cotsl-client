package com.flarelabsmc.cotsl.common.storage.player;

import com.flarelabsmc.cotsl.common.storage.type.RecordType;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

public record CharData(
        int gender,
        int headShape,
        int jawShape,
        int hair,
        int hairColor,
        int facialHair,
        int bodyType,
        int shirtColor,
        int legType,
        int pantsColor,
        int shoesType,
        int shoesColor,
        int skinColor,
        int eyesColor,
        double height
) {
    public static CharData init() {
        return new CharData(
                0,
                0, 0, 0, 0, 0, 0, 0xFFFFFF, 0,
                0xFFFFFF, 0, 0xFFFFFF, 0,
                0, 1.0
        );
    }

    public Builder rebuild() {
        return new Builder(this);
    }

    public static class Builder {
        private int gender;
        private int headShape, jawShape, hair, hairColor, facialHair, bodyType;
        private int shirtColor, legType, pantsColor, shoesType, shoesColor;
        private int skinColor, eyesColor;
        private double height;

        public Builder(CharData data) {
            this.gender = data.gender;
            this.headShape = data.headShape;
            this.jawShape = data.jawShape;
            this.hair = data.hair;
            this.hairColor = data.hairColor;
            this.facialHair = data.facialHair;
            this.bodyType = data.bodyType;
            this.shirtColor = data.shirtColor;
            this.legType = data.legType;
            this.pantsColor = data.pantsColor;
            this.shoesType = data.shoesType;
            this.shoesColor = data.shoesColor;
            this.skinColor = data.skinColor;
            this.eyesColor = data.eyesColor;
            this.height = data.height;
        }

        public Builder gender(int gender) { this.gender = gender; return this; }
        public Builder headShape(int headShape) { this.headShape = headShape; return this; }
        public Builder jawShape(int jawShape) { this.jawShape = jawShape; return this; }
        public Builder hair(int hair) { this.hair = hair; return this; }
        public Builder hairColor(int hairColor) { this.hairColor = hairColor; return this; }
        public Builder facialHair(int facialHair) { this.facialHair = facialHair; return this; }
        public Builder bodyType(int bodyType) { this.bodyType = bodyType; return this; }
        public Builder shirtColor(int shirtColor) { this.shirtColor = shirtColor; return this; }
        public Builder legType(int legType) { this.legType = legType; return this; }
        public Builder pantsColor(int pantsColor) { this.pantsColor = pantsColor; return this; }
        public Builder shoesType(int shoesType) { this.shoesType = shoesType; return this; }
        public Builder shoesColor(int shoesColor) { this.shoesColor = shoesColor; return this; }
        public Builder skinColor(int skinColor) { this.skinColor = skinColor; return this; }
        public Builder eyesColor(int eyesColor) { this.eyesColor = eyesColor; return this; }
        public Builder height(double height) { this.height = height; return this; }

        public CharData build() {
            return new CharData(
                    gender,
                    headShape, jawShape, hair, hairColor, facialHair,
                    bodyType, shirtColor, legType, pantsColor,
                    shoesType, shoesColor, skinColor,
                    eyesColor, height
            );
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
                return RecordType.getSingleton().getObjectMapper().readValue(json, CharData.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to decode CharData", e);
            }
        }

        @Override
        public void encode(ByteBuf buffer, CharData data) {
            try {
                String json = RecordType.getSingleton().getObjectMapper().writeValueAsString(data);
                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                ByteBufCodecs.VAR_INT.encode(buffer, bytes.length);
                buffer.writeBytes(bytes);
            } catch (Exception e) {
                throw new RuntimeException("Failed to encode CharData", e);
            }
        }
    };
}
