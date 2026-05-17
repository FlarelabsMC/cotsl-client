package com.flarelabsmc.cotsl.core.transform;

import com.flarelabsmc.cotsl.common.sound.CotSLSoundEvents;
import com.flarelabsmc.cotsl.core.transform.duck.AbstractHorseDuck;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
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
            instance.playSound(
                    soundEvent,
                    0.5f + (instance.level().getRandom().nextFloat() * 0.8f),
                    0.8f + (instance.level().getRandom().nextFloat() * 0.2f)
            );
            instance.playSound(
                    SoundEvents.FIRE_AMBIENT,
                    0.5f + (instance.level().getRandom().nextFloat() * 0.8f),
                    0.8f + (instance.level().getRandom().nextFloat() * 0.2f)
            );
        }
    }

    public static class AbstractHorseMixin {
        private boolean wasBeingRidden = false;
        private float speed, turnVel,
                lastFInput, lastSInput,
                deltaYRot, horseLastYRot;

        public void getRiddenRotation(AbstractHorse horse, LivingEntity controller, CallbackInfoReturnable<Vec2> cir) {
            if (!(controller instanceof Player player)) return;

            if (!wasBeingRidden) {
                turnVel = 0.0f;
                wasBeingRidden = true;
            }

            boolean isMoving = horse.walkAnimation.isMoving() || lastFInput != 0;
            float targetAngVel = -lastSInput * 5.0f * (isMoving ? 1.0f : 1.25f);

            turnVel += (targetAngVel - turnVel) * 0.25f;
            if (Math.abs(turnVel) < 0.05f) turnVel = 0.0f;

            float newYRot = Mth.wrapDegrees(horse.getYRot() + turnVel);

            cir.setReturnValue(new Vec2(player.getXRot() * 0.5f, newYRot));
        }


        public void tick(AbstractHorse horse, CallbackInfo ci) {
            if (wasBeingRidden && horse.getControllingPassenger() == null) {
                wasBeingRidden = false;
                speed = 0.0f;
                turnVel = 0.0f;
            }

            AbstractHorseDuck duck = (AbstractHorseDuck) horse;
            duck.setXV(horse.getDeltaMovement().x);
            duck.setYV(horse.getDeltaMovement().y);
            duck.setZV(horse.getDeltaMovement().z);
            duck.setVel(speed);
            duck.setTurnRate(turnVel);
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

        public void getPassengerAttachmentPoint(AbstractHorse horse, float standAnimO, Entity passenger, EntityDimensions dimensions, float scale, CallbackInfoReturnable<Vec3> cir) {
            float passengerOffset = 0.0f;

            if (horse.getPassengers().size() > 1) {
                int index = horse.getPassengers().indexOf(passenger);

                if (index == 0) passengerOffset = 0.2f;
                else passengerOffset = -0.6f;

                if (passenger instanceof Animal) passengerOffset += 0.2f;
            }

            Vec3 pos = new Vec3(0, /*yOffset + */1.40, passengerOffset/* + zOffset*/).add((new Vec3(0.0F, 0.15 * standAnimO * scale, -0.7 * standAnimO * scale)));
            cir.setReturnValue(pos.yRot(-horse.getYRot() * ((float) Math.PI / 180f)));
        }

        public void positionRider(AbstractHorse horse, Entity passenger, Entity.MoveFunction moveFunction) {
            passenger.setYRot(passenger.getYRot() + this.deltaYRot);
            passenger.setYHeadRot(passenger.getYHeadRot() + this.deltaYRot);

            if (passenger instanceof Animal a) {
                int rotationOffset = passenger.getId() % 2 == 0 ? 90 : 270;
                passenger.setYBodyRot(a.yBodyRot + (float) rotationOffset);
                passenger.setYHeadRot(passenger.getYHeadRot() + (float) rotationOffset);
            }

            deltaYRot = Mth.wrapDegrees(horse.getYRot() - horseLastYRot);
            horseLastYRot = horse.getYRot();
        }
    }
}
