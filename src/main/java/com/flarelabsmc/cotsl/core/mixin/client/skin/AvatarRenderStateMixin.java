package com.flarelabsmc.cotsl.core.mixin.client.skin;

import com.flarelabsmc.cotsl.client.render.skin.AvatarRenderStateExt;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;

import java.util.UUID;

@Mixin(AvatarRenderState.class)
public class AvatarRenderStateMixin implements AvatarRenderStateExt {
    private UUID uuid;
    private int mouthPose;
    private String currentSpeechKey;
    private float speechProgress;

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
    public String getCurrentSpeech() {
        return currentSpeechKey;
    }

    @Override
    public void setCurrentSpeech(String key) {
        this.currentSpeechKey = key;
    }

    @Override
    public float getSpeechProgress() {
        return speechProgress;
    }

    @Override
    public void setSpeechProgress(float progress) {
        this.speechProgress = progress;
    }
}
