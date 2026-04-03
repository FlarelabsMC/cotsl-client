package com.flarelabsmc.cotsl.client.render.layers;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

public class FireRenderLayer<S extends EntityRenderState, M extends EntityModel<S>> extends EnergySwirlLayer<S, M> {
    private final Supplier<M> model;

    public FireRenderLayer(RenderLayerParent<S, M> renderer, Supplier<M> model) {
        super(renderer);
        this.model = model;
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
        return model.get();
    }

    public static <S extends EntityRenderState, M extends EntityModel<S>> FireRenderLayer<S, M> createFor(S state, M model) {
        return new FireRenderLayer<>(() -> model, () -> model);
    }
}
