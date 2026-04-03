package com.flarelabsmc.cotsl.common.registry;

import com.flarelabsmc.cotsl.client.particle.options.FireSparkParticleOptions;
import com.flarelabsmc.cotsl.common.CotSL;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ParticleRegistry {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, CotSL.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FIRE_FLAME =
            PARTICLE_TYPES.register("fire_flame", () -> new SimpleParticleType(false));

    public static final DeferredHolder<ParticleType<?>, ParticleType<FireSparkParticleOptions>> FIRE_SPARK =
            PARTICLE_TYPES.register("fire_spark", () -> new ParticleType<>(false) {
                @Override
                public MapCodec<FireSparkParticleOptions> codec() {
                    return FireSparkParticleOptions.CODEC;
                }

                @Override
                public StreamCodec<? super ByteBuf, FireSparkParticleOptions> streamCodec() {
                    return FireSparkParticleOptions.STREAM_CODEC;
                }
            });
}
