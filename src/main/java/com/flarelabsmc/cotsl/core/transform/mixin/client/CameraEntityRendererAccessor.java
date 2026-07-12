package com.flarelabsmc.cotsl.core.transform.mixin.client;

import com.github.exopandora.shouldersurfing.client.renderer.CameraEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CameraEntityRenderer.class)
public interface CameraEntityRendererAccessor {
    @Accessor("isRenderingCameraEntity")
    void setRenderingCameraEntity(boolean is);

    @Accessor("isRenderingCameraEntity")
    boolean isActuallyRenderingCameraEntity();
}
