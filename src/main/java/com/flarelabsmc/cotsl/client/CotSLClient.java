package com.flarelabsmc.cotsl.client;

import com.flarelabsmc.cotsl.client.render.CotSLEntityRenderers;
import com.flarelabsmc.cotsl.client.render.skin.layers.PlayerEyeRenderLayer;
import com.flarelabsmc.cotsl.common.CotSL;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(value = CotSL.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber
public class CotSLClient {
    public CotSLClient(ModContainer container) {
        CotSLEntityRenderers.init();
        ;
    }

    @SubscribeEvent
    public static void registerModelLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(PlayerEyeRenderLayer.PlayerEyeModel.MODEL_LAYER, PlayerEyeRenderLayer.PlayerEyeModel::createLayer);
    }
}
