package com.flarelabsmc.cotsl.client.render.skin.layers.model;

import com.flarelabsmc.cotsl.client.render.texture.Frankenstein;
import com.flarelabsmc.cotsl.common.CotSL;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Optional;

public class HairModel extends GeoModel<GeoAnimatable> {
    private int style = 0;
    private Identifier texture;
    private Identifier texFallback;
    private Identifier model;
    private Identifier modelFallback;
    private Identifier anim;
    private Identifier animFallback;

    public HairModel(Identifier texture, Identifier model) {
        this.style = 0;
        this.texture = texture;
        this.texFallback = Identifier.fromNamespaceAndPath("cotsl", "skin/hair/hair_" + style);
        this.model = model;
        this.modelFallback = Identifier.fromNamespaceAndPath("cotsl", "skin/hair/hair_0");
        this.anim = Identifier.fromNamespaceAndPath("cotsl", "skin/hair/hair_" + style);
        this.animFallback = Identifier.fromNamespaceAndPath("cotsl", "skin/hair/hair_0");
    }

    public int getStyle() {
        return style;
    }

    public void setStyle(int style) {
        this.style = style;
    }

    public boolean setTexture(Identifier texture) {
        Identifier tex = texture.withPath("textures/" + texture.getPath() + ".png");
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        try {
            Optional<Resource> resource = manager.getResource(tex);
            if (resource.isEmpty()) return false;
            NativeImage img = NativeImage.read(resource.get().open());
            this.texture = tex;
            Frankenstein.updateTexture(tex, img);
            return true;
        } catch (Exception e) {
            CotSL.LOGGER.warn("Failed to load texture {}: {}", tex, e.getMessage());
            return false;
        }
    }

    public void setModel(Identifier model) {
        this.model = model;
    }

    @Override public Identifier getModelResource(GeoRenderState s) { return model; }
    @Override public Identifier getTextureResource(GeoRenderState s) { return texture; }
    @Override public Identifier getAnimationResource(GeoAnimatable a) { return anim; }
}
