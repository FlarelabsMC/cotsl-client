package com.flarelabsmc.cotsl.core.mixin.client.skin;

import com.flarelabsmc.cotsl.client.render.skin.AvatarRenderStateExt;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;

import java.util.UUID;

@Mixin(AvatarRenderState.class)
public class AvatarRenderStateMixin implements AvatarRenderStateExt {
    private UUID uuid;
    private int mouthPose;
    private float health;
    private float maxHealth;

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public int getMouthPose() {
        return mouthPose;
    }

    @Override
    public void setMouthPose(int pose) {
        this.mouthPose = pose;
    }

    @Override
    public float getHealth() {
        return health;
    }

    @Override
    public void setHealth(float health) {
        this.health = health;
    }

    @Override
    public float getMaxHealth() {
        return maxHealth;
    }

    @Override
    public void setMaxHealth(float maxHealth) {
        this.maxHealth = maxHealth;
    }
}
