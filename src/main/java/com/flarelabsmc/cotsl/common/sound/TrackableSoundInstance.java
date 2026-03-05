package com.flarelabsmc.cotsl.common.sound;

import com.flarelabsmc.cotsl.common.CotSL;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.NoSuchElementException;

/**
 * a sound instance that you can track the progress and duration of somewhat accurately. it can also follow any entity.
 */
public class TrackableSoundInstance extends AbstractSoundInstance implements TickableSoundInstance {
    private final long startTime;
    private final Entity trackingEntity;
    private final double x, y, z;
    private float lastProgress = 0f;
    private long timePaused = 0;
    private long pausedSince = 0;

    public TrackableSoundInstance(Holder<SoundEvent> sound, SoundSource source, @Nullable Entity entity, double fallbackX, double fallbackY, double fallbackZ) {
        super(sound.value(), source, SoundInstance.createUnseededRandom());
        this.startTime = System.currentTimeMillis();
        this.trackingEntity = entity;
        this.x = fallbackX;
        this.y = fallbackY;
        this.z = fallbackZ;
    }

    public TrackableSoundInstance(Holder<SoundEvent> sound, SoundSource source, double x, double y, double z) {
        super(sound.value(), source, SoundInstance.createUnseededRandom());
        this.startTime = System.currentTimeMillis();
        this.trackingEntity = null;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public long getStartTime() {
        return startTime;
    }

    /**
     * assumes the sound's sample rate is 44.1kHz.<br>
     * see /src/main/resources/assets/cotsl/sound_guidelines.txt<br>
     * also see <a href="https://stackoverflow.com/questions/20794204/how-to-determine-length-of-ogg-file">this Stack Overflow post</a>
     * @return the duration of the sound in seconds
     * */
    public double getDuration() throws NoSuchElementException {
        Identifier originalId = this.sound.getLocation();
        Identifier id = Identifier.fromNamespaceAndPath(originalId.getNamespace(), "sounds/" + originalId.getPath() + ".ogg");
        try {
            InputStream stream = Minecraft.getInstance().getResourceManager().getResource(id).get().open();
            int rate = -1;
            int length = -1;
            byte[] t = stream.readAllBytes();
            int size = t.length;
            for (int i = size-1-8-2-4; i>=0 && length<0; i--) {
                if (
                    t[i]==     (byte)'O'
                    && t[i+1]==(byte)'g'
                    && t[i+2]==(byte)'g'
                    && t[i+3]==(byte)'S'
                ) {
                    byte[] byteArray = new byte[]{t[i+6],t[i+7],t[i+8],t[i+9],t[i+10],t[i+11],t[i+12],t[i+13]};
                    ByteBuffer bb = ByteBuffer.wrap(byteArray);
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                    length = bb.getInt(0);
                }
            }
            for (int i = 0; i<size-8-2-4 && rate<0; i++) {
                if (
                        t[i]==     (byte)'v'
                        && t[i+1]==(byte)'o'
                        && t[i+2]==(byte)'r'
                        && t[i+3]==(byte)'b'
                        && t[i+4]==(byte)'i'
                        && t[i+5]==(byte)'s'
                ) {
                    byte[] byteArray = new byte[]{t[i+11],t[i+12],t[i+13],t[i+14]};
                    ByteBuffer bb = ByteBuffer.wrap(byteArray);
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                    rate = bb.getInt(0);
                }
            }
            stream.close();

            return (double) length / rate;
        } catch (NoSuchElementException | IOException e) {
            throw new NoSuchElementException("Resource not found when trying to get duration of sound: " + id, e);
        }
    }

    /**
     * @return the progress of the sound in seconds
     */
    public float getProgress() {
        updateState();
        if (Minecraft.getInstance().isPaused()) return lastProgress;
        long now = System.currentTimeMillis();
        lastProgress = (now - startTime - timePaused) / 1000f;
        return lastProgress;
    }

    @Override
    public double getX() {
        return trackingEntity != null && trackingEntity.isAlive() ? trackingEntity.getX() : x;
    }

    @Override
    public double getY() {
        return trackingEntity != null && trackingEntity.isAlive() ? trackingEntity.getY() + trackingEntity.getEyeHeight() : y;
    }

    @Override
    public double getZ() {
        return trackingEntity != null && trackingEntity.isAlive() ? trackingEntity.getZ() : z;
    }

    @Override
    public boolean isStopped() {
        updateState();
        long elapsed = System.currentTimeMillis() - startTime - timePaused;
        if (Minecraft.getInstance().isPaused()) elapsed = (long) (lastProgress * 1000f);

        try {
            return elapsed > getDuration() * 1000f;
        } catch (NoSuchElementException e) {
            CotSL.LOGGER.warn("Could not determine duration of sound {}, assuming it is not stopped.", this.sound.getLocation(), e);
            return false;
        }
    }

    @Override
    public void tick() {}

    private void updateState() {
        boolean paused = Minecraft.getInstance().isPaused();
        if (paused && pausedSince == 0) {
            pausedSince = System.currentTimeMillis();
        } else if (!paused && pausedSince != 0) {
            timePaused += System.currentTimeMillis() - pausedSince;
            pausedSince = 0;
        }
    }
}