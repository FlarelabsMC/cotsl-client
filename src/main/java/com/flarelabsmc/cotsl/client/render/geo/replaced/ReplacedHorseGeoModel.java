package com.flarelabsmc.cotsl.client.render.geo.replaced;

import com.flarelabsmc.cotsl.client.entity.replaced.ReplacedHorse;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

public class ReplacedHorseGeoModel extends GeoModel<ReplacedHorse> {
    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath("cotsl", "entity/horse");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath("cotsl", "textures/entity/horse/white_horse.png");
    }

    @Override
    public Identifier getAnimationResource(ReplacedHorse animatable) {
        return Identifier.fromNamespaceAndPath("cotsl", "entity/horse");
    }
}
