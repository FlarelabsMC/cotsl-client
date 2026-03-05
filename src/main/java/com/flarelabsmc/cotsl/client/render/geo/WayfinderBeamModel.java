package com.flarelabsmc.cotsl.client.render.geo;

import com.flarelabsmc.cotsl.common.entity.fx.WayfinderBeam;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

public class WayfinderBeamModel extends GeoModel<WayfinderBeam> {
    @Override
    public Identifier getModelResource(GeoRenderState geoRenderState) {
        return Identifier.fromNamespaceAndPath("cotsl", "entity/wayfinder_beam");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState geoRenderState) {
        return Identifier.fromNamespaceAndPath("cotsl", "textures/entity/wayfinder_beam.png");
    }

    @Override
    public Identifier getAnimationResource(WayfinderBeam wayfinderBeam) {
        return Identifier.fromNamespaceAndPath("cotsl", "entity/wayfinder_beam");
    }
}
