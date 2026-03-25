package com.flarelabsmc.cotsl.client.render.layers;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

public class FireRenderLayer<S extends EntityRenderState, M extends EntityModel<S>> extends EnergySwirlLayer<S, M> {
    public FireRenderLayer(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    @Override
    protected boolean isPowered(S s) {
        return s.displayFireAnimation;
    }

    @Override
    protected float xOffset(float v) {
        return 0;
    }

    @Override
    protected Identifier getTextureLocation() {
        return Identifier.fromNamespaceAndPath("cotsl", "textures/entity/all/fire.png");
    }

    @Override
    protected M model() {
        return this.getParentModel();
    }

    public static <S extends EntityRenderState, M extends EntityModel<S>> FireRenderLayer<S, M> createFor(S state, M model) {
        return new FireRenderLayer<>(() -> model);
    }
}
