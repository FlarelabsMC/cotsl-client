package com.flarelabsmc.cotsl.client;

import com.flarelabsmc.cotsl.client.particle.FireFlameParticle;
import com.flarelabsmc.cotsl.client.render.CotSLEntityRenderers;
import com.flarelabsmc.cotsl.client.render.skin.layers.PlayerHandsRenderLayer;
import com.flarelabsmc.cotsl.client.render.skin.layers.PlayerEyeRenderLayer;
import com.flarelabsmc.cotsl.client.render.skin.layers.PlayerEyebrowRenderLayer;
import com.flarelabsmc.cotsl.client.render.skin.layers.PlayerMouthRenderLayer;
import com.flarelabsmc.cotsl.client.render.texture.Frankenstein;
import com.flarelabsmc.cotsl.client.speech.SpeechData;
import com.flarelabsmc.cotsl.common.CotSL;
import com.flarelabsmc.cotsl.common.registry.ParticleRegistry;
import com.flarelabsmc.cotsl.launch.LauncherWindow;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@Mod(value = CotSL.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CotSL.MOD_ID, value = Dist.CLIENT)
public class CotSLClient {
    public CotSLClient(ModContainer container) {
        CotSLEntityRenderers.init();
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        SpeechData.load();
    }

    @SubscribeEvent
    public static void registerModelLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(PlayerEyeRenderLayer.PlayerEyeModel.MODEL_LAYER, PlayerEyeRenderLayer.PlayerEyeModel::createLayer);
        event.registerLayerDefinition(PlayerEyebrowRenderLayer.PlayerEyebrowModel.MODEL_LAYER, PlayerEyebrowRenderLayer.PlayerEyebrowModel::createLayer);
        event.registerLayerDefinition(PlayerMouthRenderLayer.PlayerMouthModel.MODEL_LAYER, PlayerMouthRenderLayer.PlayerMouthModel::createLayer);
        event.registerLayerDefinition(PlayerHandsRenderLayer.LeftHandModel.MODEL_LAYER, PlayerHandsRenderLayer.LeftHandModel::createLayer);
        event.registerLayerDefinition(PlayerHandsRenderLayer.RightHandModel.MODEL_LAYER, PlayerHandsRenderLayer.RightHandModel::createLayer);
    }

    @SubscribeEvent
    public static void registerReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(Identifier.parse("cotsl:clear_cache"),
                (ResourceManagerReloadListener) manager -> Frankenstein.reset());
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticleRegistry.FIRE_FLAME.get(), FireFlameParticle.FireFlameProvider::new);
    }
}
