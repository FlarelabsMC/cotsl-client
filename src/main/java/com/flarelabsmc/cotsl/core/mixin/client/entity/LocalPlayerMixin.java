package com.flarelabsmc.cotsl.core.mixin.client.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer {
    public LocalPlayerMixin(ClientLevel clientLevel, GameProfile gameProfile) {
        super(clientLevel, gameProfile);
    }

    @Inject(method = "shouldStopRunSprinting", at = @At("RETURN"), cancellable = true)
    private void shouldStopRunSprinting(CallbackInfoReturnable<Boolean> cir) {
        Options o = Minecraft.getInstance().options;
        cir.setReturnValue(o.sprintWindow().get() > 0 ? cir.getReturnValueZ() : cir.getReturnValueZ() || !o.keySprint.isDown());
    }

    @Inject(method = "shouldStopSwimSprinting", at = @At("RETURN"), cancellable = true)
    private void shouldStopSwimSprinting(CallbackInfoReturnable<Boolean> cir) {
        Options o = Minecraft.getInstance().options;
        cir.setReturnValue(o.sprintWindow().get() > 0 ? cir.getReturnValueZ() : cir.getReturnValueZ() || !o.keySprint.isDown() || this.horizontalCollision);
    }
}
