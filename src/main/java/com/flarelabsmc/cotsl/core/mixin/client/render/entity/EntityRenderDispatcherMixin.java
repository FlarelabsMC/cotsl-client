package com.flarelabsmc.cotsl.core.mixin.client.render.entity;

import com.flarelabsmc.cotsl.common.registry.ParticleRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Redirect(method = "submit", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitFlame(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lorg/joml/Quaternionf;)V"))
    private void sub(SubmitNodeCollector instance, PoseStack poseStack, EntityRenderState entityRenderState, Quaternionf quaternionf) {
        if (!entityRenderState.displayFireAnimation || Minecraft.getInstance().isPaused()) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        RandomSource random = level.getRandom();
        if (random.nextFloat() > 0.05f) return;
        float hw = entityRenderState.boundingBoxWidth * 0.5f;
        float height = entityRenderState.boundingBoxHeight;
        double x = entityRenderState.x;
        double y = entityRenderState.y;
        double z = entityRenderState.z;
        for (int i = 0; i < 2; i++) {
            double px = x + (random.nextDouble() * 2.0 - 1.0) * hw;
            double py = y + random.nextDouble() * height;
            double pz = z + (random.nextDouble() * 2.0 - 1.0) * hw;
            double vy = 0.04 + random.nextDouble() * 0.04;
            level.addParticle(random.nextBoolean() ? ParticleRegistry.FIRE_FLAME.get() : ParticleTypes.CAMPFIRE_COSY_SMOKE, px, py, pz, 0.0, vy, 0.0);
        }
    }
}
