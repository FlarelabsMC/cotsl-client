package com.flarelabsmc.cotsl.core.hotmixin;

import com.flarelabsmc.cotsl.common.sound.CotSLSoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class MixinsCommon {
    public static class PlayerMixin {
        public void getHurtSound(DamageSource source, CallbackInfoReturnable<SoundEvent> cir) {
            cir.setReturnValue(CotSLSoundEvents.MALE_HURT.value());
        }
    }

    public static class LivingEntityMixin {
        public void handleDamageEvent(LivingEntity instance, SoundEvent soundEvent, float volume, float pitch) {
            instance.playSound(soundEvent, 0.5f + (instance.level().getRandom().nextFloat() * 0.8f), 0.8f + (instance.level().getRandom().nextFloat() * 0.2f));
            instance.playSound(SoundEvents.FIRE_AMBIENT, 0.5f + (instance.level().getRandom().nextFloat() * 0.8f), 0.8f + (instance.level().getRandom().nextFloat() * 0.2f));
        }
    }

    public static class AbstractHorseMixin {
        public boolean wasBeingRidden = false;
        public float speed = 0.0f;
        private float angularVelocity = 0.0f;
        private float lastFInput = 0.0f;
        private float lastSInput = 0.0f;

        public void getRiddenRotation(AbstractHorse horse, LivingEntity controller, CallbackInfoReturnable<Vec2> cir) {
            if (!(controller instanceof Player player)) return;
            if (!wasBeingRidden) {
                angularVelocity = 0.0f;
                wasBeingRidden = true;
            }
            boolean isMoving = horse.walkAnimation.isMoving() || lastFInput != 0;
            float targetAngVel = -lastSInput * 5.0f * (isMoving ? 1.0f : 1.25f);
            angularVelocity += (targetAngVel - angularVelocity) * 0.25f;
            if (Math.abs(angularVelocity) < 0.05f) angularVelocity = 0.0f;
            float newYRot = Mth.wrapDegrees(horse.getYRot() + angularVelocity);
            cir.setReturnValue(new Vec2(player.getXRot() * 0.5f, newYRot));
        }


        public void tick(AbstractHorse horse, CallbackInfo ci) {
            if (wasBeingRidden && horse.getControllingPassenger() == null) {
                wasBeingRidden = false;
                speed = 0.0f;
                angularVelocity = 0.0f;
            }
        }

        public Vec3 getRiddenInput(AbstractHorse horse, Vec3 original, Player player, Vec3 selfInput) {
            float fInput = player.zza;
            float sInput = player.xxa;
            lastFInput = fInput;
            lastSInput = sInput;
            float tSpeed = fInput > 0 ? 1.0f : 0.0f;
            if (speed < tSpeed) speed = Math.min(speed + 0.05f, tSpeed);
            else if (speed > tSpeed) speed = Math.max(speed - 0.1f, tSpeed);
            float fMove = (fInput < 0 ? fInput * 0.25f : speed);
            return new Vec3(0, original.y, fMove);
        }
    }
}
