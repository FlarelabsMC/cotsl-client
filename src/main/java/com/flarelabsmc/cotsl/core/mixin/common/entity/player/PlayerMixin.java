package com.flarelabsmc.cotsl.core.mixin.common.entity.player;

import com.flarelabsmc.cotsl.core.hotmixin.MixinsCommon;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerMixin {
    private final MixinsCommon.PlayerMixin self = new MixinsCommon.PlayerMixin();

    @Inject(method = "getHurtSound", at = @At("HEAD"), cancellable = true)
    private void getHurtSound(DamageSource source, CallbackInfoReturnable<SoundEvent> cir) {
        self.getHurtSound(source, cir);
    }
}
