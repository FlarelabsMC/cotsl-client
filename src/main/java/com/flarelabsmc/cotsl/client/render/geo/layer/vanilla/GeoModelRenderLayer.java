package com.flarelabsmc.cotsl.client.render.geo.layer.vanilla;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoObjectRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;

public abstract class GeoModelRenderLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends RenderLayer<S, M> implements GeoAnimatable {
    private final GeoObjectRenderer<GeoAnimatable, GeoModel<?>, GeoRenderState> renderer;
    private final GeoModel<GeoAnimatable> model;

    public GeoModelRenderLayer(RenderLayerParent<S, M> renderer, GeoModel<GeoAnimatable> model) {
        super(renderer);
        this.renderer = new GeoObjectRenderer<>(model) {
            @Override
            public void adjustModelBonesForRender(RenderPassInfo<GeoRenderState> renderPassInfo, BoneSnapshots snapshots) {
                adjustBones(renderPassInfo, snapshots);
            }
        };
        this.model = model;
    }

    /**
     * note: do NOT push or pop the pose stack within this method
     */
    public abstract PoseStack modifyPose(PoseStack poseStack, S renderState, float yRot, float xRot);

    public void adjustBones(RenderPassInfo<GeoRenderState> renderPassInfo, BoneSnapshots snapshots) {};

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int i, S s, float yRot, float xRot) {
        CameraRenderState cstate = Minecraft.getInstance().gameRenderer.getLevelRenderState().cameraRenderState;
        poseStack.pushPose();

        this.modifyPose(poseStack, s, yRot, xRot);

        renderer.performRenderPass(this, model, poseStack, submitNodeCollector, cstate, i, s.outlineColor);
        poseStack.popPose();
    }
}
