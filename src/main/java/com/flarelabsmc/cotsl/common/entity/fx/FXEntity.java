package com.flarelabsmc.cotsl.common.entity.fx;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public abstract class FXEntity extends Entity implements IFXEntity {
    public FXEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public FXEntity getFXEntity() {
        return this;
    }

    public abstract int getLifetime();

    public boolean hasExpired() {
        return this.tickCount > getLifetime();
    }

    @Override
    public void checkDespawn() {
        if (shouldDespawn()) discard();
    }

    @Override
    public boolean isAlwaysTicking() {
        return true;
    }
}
