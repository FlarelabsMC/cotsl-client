package com.flarelabsmc.cotsl.core.transform.mixin.client.render.entity;

import com.flarelabsmc.cotsl.core.transform.MixinsClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    private final MixinsClient.EntityRenderDispatcherMixin self = new MixinsClient.EntityRenderDispatcherMixin();

    @Redirect(method = "submit", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitFlame(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lorg/joml/Quaternionf;)V"))
    private void submit(SubmitNodeCollector instance, PoseStack poseStack, EntityRenderState entityRenderState, Quaternionf quaternionf) {
        self.submit_submitFlame(poseStack, entityRenderState, quaternionf);
    }
}
