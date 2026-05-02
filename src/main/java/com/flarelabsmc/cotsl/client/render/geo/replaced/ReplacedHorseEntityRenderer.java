package com.flarelabsmc.cotsl.client.render.geo.replaced;

import com.flarelabsmc.cotsl.common.entity.replaced.ReplacedHorse;
import com.geckolib.renderer.GeoReplacedEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.HorseRenderState;
import net.minecraft.world.entity.animal.equine.Horse;

public class ReplacedHorseEntityRenderer extends GeoReplacedEntityRenderer<ReplacedHorse, Horse, HorseRenderState> {
    public ReplacedHorseEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new ReplacedHorseGeoModel(), new ReplacedHorse());
    }

    @Override
    public HorseRenderState createRenderState(ReplacedHorse replacedHorse, Horse entity) {
        return new HorseRenderState();
    }

    @Override
    public void extractRenderState(Horse entity, HorseRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
    }
}
