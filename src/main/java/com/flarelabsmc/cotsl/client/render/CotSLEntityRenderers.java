package com.flarelabsmc.cotsl.client.render;

import com.flarelabsmc.cotsl.client.render.geo.GeoEmissiveEntityRenderer;
import com.flarelabsmc.cotsl.common.entity.EntityRegistry;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

@EventBusSubscriber(Dist.CLIENT)
public class CotSLEntityRenderers {
    public static void init() {}

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.WAYFINDER_BEAM.get(), ctx -> new GeoEmissiveEntityRenderer<>(ctx, EntityRegistry.WAYFINDER_BEAM.get()));
    }

    public static final RenderPipeline.Snippet ENTITY_CUTOUT_NO_CULL_EMISSIVE_SNIPPET;
    public static final RenderPipeline ENTITY_CUTOUT_NO_CULL_EMISSIVE;

    static {
        ENTITY_CUTOUT_NO_CULL_EMISSIVE_SNIPPET = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_LIGHT_DIR_SNIPPET)
                .withVertexShader("core/entity")
                .withFragmentShader("core/entity")
                .withSampler("Sampler0")
                .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS)
                .withShaderDefine("EMISSIVE")
                .withShaderDefine("NO_OVERLAY")
                .withShaderDefine("NO_CARDINAL_LIGHTING")
                .buildSnippet();

        ENTITY_CUTOUT_NO_CULL_EMISSIVE = RenderPipeline.builder(ENTITY_CUTOUT_NO_CULL_EMISSIVE_SNIPPET)
                .withLocation("pipeline/entity_cutout_no_cull_emissive")
                .withShaderDefine("ALPHA_CUTOUT", 0.1f)
                .withSampler("Sampler0")
                .withSampler("Sampler2")
                .withCull(false)
                .withDepthWrite(true)
                .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                .withVertexFormat(DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS)
                .build();
    }

    @SubscribeEvent
    public static void registerRenderPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(ENTITY_CUTOUT_NO_CULL_EMISSIVE);
    }
}
