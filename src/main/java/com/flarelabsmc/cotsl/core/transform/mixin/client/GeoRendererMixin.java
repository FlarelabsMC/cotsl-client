package com.flarelabsmc.cotsl.core.transform.mixin.client;

import com.geckolib.renderer.base.GeoRenderer;
import com.github.exopandora.shouldersurfing.client.ShoulderSurfing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GeoRenderer.class)
public interface GeoRendererMixin {
    @ModifyVariable(method = "submitRenderTasks", at = @At("STORE"), name = "renderColor")
    private int renderColor(int renderColor) {
        return ShoulderSurfing.getInstance().getCameraEntityRenderer().applyCameraEntityAlphaContextAware(renderColor);
    }
}
