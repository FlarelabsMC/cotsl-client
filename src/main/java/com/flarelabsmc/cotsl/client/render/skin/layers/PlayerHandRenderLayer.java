package com.flarelabsmc.cotsl.client.render.skin.layers;

import com.flarelabsmc.cotsl.client.render.geo.layers.vanilla.GeoModelRenderLayer;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.util.GeckoLibUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class PlayerHandRenderLayer extends GeoModelRenderLayer<AvatarRenderState, PlayerModel> {
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private AvatarRenderState renderState = new AvatarRenderState();

    public PlayerHandRenderLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer, GeoModel<GeoAnimatable> model) {
        super(renderer, model);
    }

    @Override
    public void adjustBones(RenderPassInfo<GeoRenderState> renderPassInfo, BoneSnapshots snapshots) {
        snapshots.ifPresent("left_hand", shot -> modelPoseToBone(
                getParentModel().leftArm,
                shot
        ));
        snapshots.ifPresent("right_hand", shot -> modelPoseToBone(
                getParentModel().rightArm,
                shot
        ));
    }

    private void modelPoseToBone(ModelPart part, BoneSnapshot shot) {
        PoseStack dumpStack = new PoseStack();
        part.translateAndRotate(dumpStack);
        Quaternionf rotation = dumpStack.last().pose().getNormalizedRotation(new Quaternionf());
        Vector3f eulerAngles = rotation.getEulerAnglesXYZ(new Vector3f());
        Vector3f translation = dumpStack.last().pose().getTranslation(new Vector3f());
        translation.add(0, -part.cubes.getFirst().maxY, 0);
        translation.rotate(rotation);
        shot.setRotation(-eulerAngles.x, eulerAngles.y, -eulerAngles.z);
        shot.setTranslation(-translation.x, translation.y, -translation.z);
    }

    @Override
    public PoseStack modifyPose(PoseStack poseStack, AvatarRenderState renderState, float yRot, float xRot) {
        this.renderState = renderState;
        poseStack.translate(0.45, 1.15, -0.5);
        poseStack.mulPose(Axis.XP.rotationDegrees(180));
        poseStack.mulPose(Axis.YP.rotationDegrees(180));
        return poseStack;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
