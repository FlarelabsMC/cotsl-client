package com.flarelabsmc.cotsl.client.render.geo;

import com.flarelabsmc.cotsl.client.render.CotSLEntityRenderers;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.base.GeoRenderState;

public class GeoEmissiveEntityRenderer<T extends Entity & GeoAnimatable, R extends EntityRenderState & GeoRenderState> extends GeoEntityRenderer<T, R> {
    public GeoEmissiveEntityRenderer(EntityRendererProvider.Context context, EntityType<? extends T> entityType) {
        super(context, entityType);
    }

    public final Identifier TEXTURE = Identifier.fromNamespaceAndPath("cotsl", "textures/entity/wayfinder_beam.png");

    public final RenderType RENDER_TYPE = RenderType.create("entity_cutout_no_cull_emissive", RenderSetup.builder(CotSLEntityRenderers.ENTITY_CUTOUT_NO_CULL_EMISSIVE)
            .withTexture("Sampler0", TEXTURE)
            .affectsCrumbling()
            .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
            .createRenderSetup()
    );

    @Override
    public RenderType getRenderType(R renderState, Identifier texture) {
        return RENDER_TYPE;
    }

    @Override
    public boolean shouldRender(T livingEntity, Frustum camera, double camX, double camY, double camZ) {
        return livingEntity != null;
    }
}
