package com.flarelabsmc.cotsl.client.render.skin.layers;

import com.flarelabsmc.cotsl.client.render.geo.layer.vanilla.GeoModelRenderLayer;
import com.flarelabsmc.cotsl.client.render.skin.layers.model.HairModel;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.util.GeckoLibUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class HairRenderLayer extends GeoModelRenderLayer<AvatarRenderState, PlayerModel> {
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    public final HairModel model;
    private AvatarRenderState renderState = new AvatarRenderState();
    private float yRotO;
    private float vel;
    private double lastY;
    private float currentRotX;
    private float currentRotY;
    private float currentRotZ;

    public HairRenderLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer, HairModel model) {
        super(renderer, model);
        this.model = model;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>("Default", 10, (test) -> {
            test.setControllerSpeed(1);
            if (renderState.walkAnimationSpeed > 0.01f) {
                test.setControllerSpeed(Math.max(renderState.walkAnimationSpeed, 0.5f));
                return test.setAndContinue(DefaultAnimations.WALK);
            } else if (renderState.walkAnimationSpeed < 0.01f) {
                return test.setAndContinue(DefaultAnimations.IDLE);
            }
            return PlayState.STOP;
        }));
    }

    @Override
    public void adjustBones(RenderPassInfo<GeoRenderState> renderPassInfo, BoneSnapshots snapshots) {
        float diff = renderState.yRot - yRotO;
        vel += ((diff * 2) - vel * 0.5f) * 0.1f;
        double vMotion = renderState.xRot + (float) ((renderState.y - lastY) * 1000f);
        float targetRotZ = (float) Math.sin(vel * 0.1f);
        float targetRotX = (float) Math.min(30 * Mth.DEG_TO_RAD, vMotion * Mth.DEG_TO_RAD);
        float targetRotY = ((renderState.yRot * Mth.DEG_TO_RAD) * 0.5f);
        float lerpFactor = 0.15f;
        currentRotZ = Mth.lerp(lerpFactor, currentRotZ, targetRotZ);
        currentRotX = Mth.lerp(lerpFactor, currentRotX, targetRotX);
        currentRotY = Mth.lerp(lerpFactor, currentRotY, targetRotY);
        snapshots.ifPresent("long", snap -> {
            snap.setRotZ(currentRotZ);
            snap.setRotX(currentRotX);
            snap.skipRender(renderState.headEquipment != ItemStack.EMPTY);
        });
        snapshots.ifPresent("back_0", snap -> {
            float clmp = Mth.clamp(currentRotX, -0.4f, 0f);
            snap.setRotZ(snap.getRotZ() + currentRotZ * 0.5f);
            snap.setRotX(clmp * 0.5f);
            snap.setRotY(currentRotY * 0.5f);
            snap.skipRender(renderState.headEquipment != ItemStack.EMPTY);
        });
        yRotO = renderState.yRot;
        lastY = renderState.y;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public PoseStack modifyPose(PoseStack poseStack, AvatarRenderState renderState, float yRot, float xRot) {
        this.renderState = renderState;
        this.getParentModel().head.translateAndRotate(poseStack);
        poseStack.translate(0.5, 0.5, -0.5);
        poseStack.mulPose(Axis.XP.rotationDegrees(180f));
        poseStack.mulPose(Axis.YP.rotationDegrees(180f));
        if (renderState.headEquipment != ItemStack.EMPTY) {
            poseStack.scale(0.92f, 0.92f, 0.92f);
            poseStack.translate(0.04, 0, 0);
        }
        return poseStack;
    }
}
