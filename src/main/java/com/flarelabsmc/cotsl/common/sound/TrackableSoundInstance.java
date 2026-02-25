package com.flarelabsmc.cotsl.common.sound;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

public class TrackableSoundInstance extends AbstractSoundInstance implements TickableSoundInstance {
    private final long startTime;
    private final float duration;
    private final Entity trackingEntity;
    private final double x, y, z;

    public TrackableSoundInstance(Holder<SoundEvent> sound, float duration, SoundSource source, @Nullable Entity entity, double fallbackX, double fallbackY, double fallbackZ) {
        super(sound.value(), source, SoundInstance.createUnseededRandom());
        this.startTime = System.currentTimeMillis();
        this.duration = duration;
        this.trackingEntity = entity;
        this.x = fallbackX;
        this.y = fallbackY;
        this.z = fallbackZ;
    }

    public TrackableSoundInstance(Holder<SoundEvent> sound, float duration, SoundSource source, double x, double y, double z) {
        super(sound.value(), source, SoundInstance.createUnseededRandom());
        this.startTime = System.currentTimeMillis();
        this.duration = duration;
        this.trackingEntity = null;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public long getStartTime() {
        return startTime;
    }

    public float getDuration() {
        return duration;
    }

    public float getProgress() {
        return (System.currentTimeMillis() - startTime) / 1000f;
    }

    @Override
    public double getX() {
        AABB box = trackingEntity != null ? trackingEntity.getBoundingBox() : null;
        return trackingEntity != null && trackingEntity.isAlive() ? trackingEntity.getX() + box.getXsize() / 2 : x;
    }

    @Override
    public double getY() {
        return trackingEntity != null && trackingEntity.isAlive() ? trackingEntity.getY() + trackingEntity.getEyeHeight() : y;
    }

    @Override
    public double getZ() {
        AABB box = trackingEntity != null ? trackingEntity.getBoundingBox() : null;
        return trackingEntity != null && trackingEntity.isAlive() ? trackingEntity.getZ() + box.getZsize() / 2 : z;
    }

    @Override
    public boolean isStopped() {
        long end = startTime + (long)(duration * 1000f);
        return System.currentTimeMillis() > end;
    }

    @Override
    public void tick() {

    }
}