package com.flarelabsmc.cotsl.client.particle.options;

import com.flarelabsmc.cotsl.common.registry.ParticleRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record FireSparkParticleOptions(int lifetime, float initialScale) implements ParticleOptions {
    public static final MapCodec<FireSparkParticleOptions> CODEC = RecordCodecBuilder.mapCodec(i ->
            i.group(
                    Codec.intRange(1, Integer.MAX_VALUE).fieldOf("lifetime").forGetter(FireSparkParticleOptions::lifetime),
                    Codec.FLOAT.fieldOf("initial_scale").forGetter(FireSparkParticleOptions::initialScale)
            ).apply(i, FireSparkParticleOptions::new));

    public static final StreamCodec<ByteBuf, FireSparkParticleOptions> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, FireSparkParticleOptions::lifetime,
                    ByteBufCodecs.FLOAT, FireSparkParticleOptions::initialScale,
                    FireSparkParticleOptions::new
            );

    @Override
    public ParticleType<FireSparkParticleOptions> getType() {
        return ParticleRegistry.FIRE_SPARK.get();
    }
}