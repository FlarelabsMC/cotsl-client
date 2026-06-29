package com.flarelabsmc.cotsl.client.render;

import com.flarelabsmc.cotsl.client.render.geo.WayfinderBeamEntityRenderer;
import com.flarelabsmc.cotsl.client.render.geo.replaced.ReplacedHorseEntityRenderer;
import com.flarelabsmc.cotsl.common.entity.EntityRegistry;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
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
        event.registerEntityRenderer(
                EntityRegistry.WAYFINDER_BEAM.get(),
                ctx ->
                        new WayfinderBeamEntityRenderer<>(
                                ctx, EntityRegistry.WAYFINDER_BEAM.get()
                        )
        );
        event.registerEntityRenderer(
                EntityTypes.HORSE, ReplacedHorseEntityRenderer::new
        );
    }

    public static final RenderPipeline.Snippet ENTITY_CUTOUT_NO_CULL_EMISSIVE_SNIPPET;
    public static final RenderPipeline ENTITY_CUTOUT_NO_CULL_EMISSIVE;

    static {
        ENTITY_CUTOUT_NO_CULL_EMISSIVE_SNIPPET =
                RenderPipeline.builder(RenderPipelines.MATRICES_FOG_LIGHT_DIR_SNIPPET)
                        .withVertexShader("core/entity")
                        .withFragmentShader("core/entity")
                        .withShaderDefine("EMISSIVE")
                        .withShaderDefine("NO_OVERLAY")
                        .withShaderDefine("NO_CARDINAL_LIGHTING")
                        .withPrimitiveTopology(PrimitiveTopology.LINES)
                        .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                        .withVertexBinding(0, DefaultVertexFormat.ENTITY)
                        .buildSnippet();

        ENTITY_CUTOUT_NO_CULL_EMISSIVE =
                RenderPipeline.builder(ENTITY_CUTOUT_NO_CULL_EMISSIVE_SNIPPET)
                        .withLocation("pipeline/entity_cutout_no_cull_emissive")
                        .withShaderDefine("ALPHA_CUTOUT", 0.1f)
                        .withCull(false)
                        .withDepthStencilState(DepthStencilState.DEFAULT)
                        .withPrimitiveTopology(PrimitiveTopology.LINES)
                        .withBindGroupLayout(BindGroupLayouts.SAMPLER2)
                        .build();
    }

    @SubscribeEvent
    public static void registerRenderPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(ENTITY_CUTOUT_NO_CULL_EMISSIVE);
    }
}
