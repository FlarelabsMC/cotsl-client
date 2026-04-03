package com.flarelabsmc.cotsl.core.hotmixin;

import com.flarelabsmc.cotsl.common.sound.CotSLSoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
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
}
