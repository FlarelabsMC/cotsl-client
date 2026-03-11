package com.flarelabsmc.cotsl.client.render.skin.layers.model;

import com.flarelabsmc.cotsl.common.network.NetworkHandler;
import com.flarelabsmc.cotsl.common.storage.user.PermanentUser;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public class HandsModel extends GeoModel<GeoAnimatable> {
    private final Identifier texFallback = Identifier.fromNamespaceAndPath("cotsl", "skin/hands/hands_0");
    private final UUID playerUUID;

    public HandsModel(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    @Override
    public Identifier getModelResource(GeoRenderState geoRenderState) {
        return Identifier.fromNamespaceAndPath("cotsl", "skin/player_hands");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState geoRenderState) {
        PermanentUser user = NetworkHandler.getCachedUserData(playerUUID);
        if (user != null) {
            return Identifier.fromNamespaceAndPath("cotsl", "skin/hands/hands_" + user.getCharacterData().skinColor());
        }
        return texFallback;
    }

    @Override
    public Identifier getAnimationResource(GeoAnimatable geoAnimatable) {
        return null;
    }
}
