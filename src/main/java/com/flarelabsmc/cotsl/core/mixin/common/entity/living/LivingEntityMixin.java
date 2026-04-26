package com.flarelabsmc.cotsl.core.mixin.common.entity.living;

import com.flarelabsmc.cotsl.core.hotmixin.MixinsCommon;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    private final MixinsCommon.LivingEntityMixin self = new MixinsCommon.LivingEntityMixin();

    @Redirect(method = "handleDamageEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"))
    private void handleDamageEvent(LivingEntity instance, SoundEvent soundEvent, float volume, float pitch) {
        self.handleDamageEvent(instance, soundEvent, volume, pitch);
    }
}