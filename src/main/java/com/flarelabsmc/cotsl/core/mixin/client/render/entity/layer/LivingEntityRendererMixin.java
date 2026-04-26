package com.flarelabsmc.cotsl.core.mixin.client.render.entity.layer;

import com.flarelabsmc.cotsl.client.render.layers.FireRenderLayer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings({"unchecked", "rawtypes"})
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<
        T extends LivingEntity,
        S extends LivingEntityRenderState,
        M extends EntityModel<S>
> extends EntityRenderer<T, S> implements RenderLayerParent<S, M> {
    protected LivingEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    @Shadow
    public abstract boolean addLayer(RenderLayer<S, M> layer);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(EntityRendererProvider.Context context, EntityModel model, float shadow, CallbackInfo ci) {
        addLayer(new FireRenderLayer<>(this, this::getModel));
    }

    @Redirect(method = "getOverlayCoords", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/OverlayTexture;v(Z)I"))
    private static int getOverlayCoords(boolean hurtOverlay) {
        return OverlayTexture.v(false);
    }
}
