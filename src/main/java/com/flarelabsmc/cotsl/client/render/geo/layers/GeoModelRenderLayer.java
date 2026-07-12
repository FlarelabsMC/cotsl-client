package com.flarelabsmc.cotsl.client.render.geo.layers;

import com.flarelabsmc.cotsl.core.transform.duck.AvatarRenderStateDuck;
import com.flarelabsmc.cotsl.core.transform.mixin.client.CameraEntityRendererAccessor;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoObjectRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.github.exopandora.shouldersurfing.client.ShoulderSurfing;
import com.github.exopandora.shouldersurfing.client.renderer.CameraEntityRenderer;
import com.github.exopandora.shouldersurfing.client.renderer.rendertype.ShoulderSurfingRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;

public abstract class GeoModelRenderLayer<
        S extends AvatarRenderState,
        M extends PlayerModel
> extends RenderLayer<S, M> implements GeoAnimatable {
    private final GeoObjectRenderer<GeoAnimatable, GeoModel<?>, GeoRenderState> renderer;
    private final GeoModel<GeoAnimatable> model;

    public GeoModelRenderLayer(RenderLayerParent<S, M> renderer, GeoModel<GeoAnimatable> model) {
        super(renderer);
        this.renderer = new GeoObjectRenderer<>(model) {
            @Override
            public void adjustModelBonesForRender(RenderPassInfo<GeoRenderState> renderPassInfo,
                                                  BoneSnapshots snapshots) {
                adjustBones(renderPassInfo, snapshots);
            }

            @Override
            public RenderType getRenderType(GeoRenderState state, Identifier texture) {
                return ShoulderSurfingRenderTypes.entityTranslucentItemTarget(texture);
            }
        };
        this.model = model;
    }

    /**
     * note: do NOT push or pop the pose stack within this method
     */
    public abstract PoseStack modifyPose(PoseStack poseStack,
                                         S renderState,
                                         float yRot,
                                         float xRot);

    public void adjustBones(RenderPassInfo<GeoRenderState> renderPassInfo, BoneSnapshots snapshots) {};

    @Override
    public void submit(PoseStack poseStack,
                       SubmitNodeCollector submitNodeCollector,
                       int i,
                       S s,
                       float yRot,
                       float xRot) {
        CameraEntityRendererAccessor car = (CameraEntityRendererAccessor) ShoulderSurfing.getInstance().getCameraEntityRenderer();
        car.setRenderingCameraEntity(true);
        CameraRenderState cstate = Minecraft.getInstance().gameRenderer.gameRenderState().levelRenderState.cameraRenderState;

        poseStack.pushPose();

        this.modifyPose(poseStack, s, yRot, xRot);

        renderer.performRenderPass(this, model, poseStack, submitNodeCollector, cstate, i, s.outlineColor);
        poseStack.popPose();
        car.setRenderingCameraEntity(false);
    }
}
