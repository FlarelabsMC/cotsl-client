package com.flarelabsmc.cotsl.common.entity;

import com.flarelabsmc.cotsl.common.CotSL;
import com.flarelabsmc.cotsl.common.entity.fx.WayfinderBeam;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class EntityRegistry {
    public static void init() {}

    public static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities(CotSL.MOD_ID);

    public static final Supplier<EntityType<WayfinderBeam>> WAYFINDER_BEAM = ENTITY_TYPES.registerEntityType(
            "wayfinder_beam", WayfinderBeam::new, MobCategory.MISC,
            builder -> builder
                    .sized(0f, 0f)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .updateInterval(100)
    );
}
