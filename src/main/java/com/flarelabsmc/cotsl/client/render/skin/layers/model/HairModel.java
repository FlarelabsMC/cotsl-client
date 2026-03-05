package com.flarelabsmc.cotsl.client.render.skin.layers.model;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public class HairModel extends GeoModel<GeoAnimatable> {
    private int style = 0;
    private Identifier texture;
    private Identifier texFallback;
    private Identifier model;
    private Identifier modelFallback;

    public HairModel(Identifier texture, Identifier model) {
        this.style = 0;
        this.texture = texture;
        this.texFallback = Identifier.fromNamespaceAndPath("cotsl", "skin/hair/hair_" + style);
        this.model = model;
        this.modelFallback = Identifier.fromNamespaceAndPath("cotsl", "skin/hair/hair_0");
    }

    public int getStyle() {
        return style;
    }

    public void setStyle(int style) {
        this.style = style;
    }

    public void setTexture(Identifier texture) {
        this.texture = texture;
    }

    public void setModel(Identifier model) {
        this.model = model;
    }

    @Override
    public Identifier getModelResource(GeoRenderState geoRenderState) {
        return model;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState geoRenderState) {
        return texture;
    }

    @Override
    public Identifier getAnimationResource(GeoAnimatable geoAnimatable) {
        return null;
    }
}
