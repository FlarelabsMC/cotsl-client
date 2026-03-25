package com.flarelabsmc.cotsl.common.registry;

import com.flarelabsmc.cotsl.common.CotSL;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ParticleRegistry {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, CotSL.MOD_ID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FIRE_FLAME =
            PARTICLE_TYPES.register("fire_flame", () -> new SimpleParticleType(false));
}
