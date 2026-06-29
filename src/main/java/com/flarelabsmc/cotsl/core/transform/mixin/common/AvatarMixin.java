package com.flarelabsmc.cotsl.core.transform.mixin.common;

import com.flarelabsmc.cotsl.core.transform.duck.AvatarDuck;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Avatar.class)
public class AvatarMixin implements AvatarDuck {
    @Unique
    private boolean wasMoving;

    @Override
    public void setWasMoving(boolean wasMoving) {
        this.wasMoving = wasMoving;
    }

    @Override
    public boolean wasMoving() {
        return wasMoving;
    }
}
