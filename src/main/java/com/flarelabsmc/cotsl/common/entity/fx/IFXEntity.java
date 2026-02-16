package com.flarelabsmc.cotsl.common.entity.fx;

import net.minecraft.world.entity.Entity;

public interface IFXEntity {
    default boolean shouldDespawn() {
        if (getFXEntity().hasExpired()) return true;

        Entity entity = getFXEntity().level().getNearestPlayer(getFXEntity(), -1.0);
        if (entity != null) {
            double dist = entity.distanceToSqr(getFXEntity());
            int despawnDist = getFXEntity().getType().getCategory().getDespawnDistance();
            int despawnDistSq = despawnDist * despawnDist;
            return dist > despawnDistSq;
        }
        return false;
    }
    FXEntity getFXEntity();
}
