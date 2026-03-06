package com.flarelabsmc.cotsl.client.render.skin.layers;

import com.flarelabsmc.cotsl.client.render.geo.layer.vanilla.GeoModelRenderLayer;
import com.flarelabsmc.cotsl.client.render.skin.layers.model.HairModel;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.util.GeckoLibUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public class HairRenderLayer extends GeoModelRenderLayer<AvatarRenderState, PlayerModel> {
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    public final HairModel model;
    private AvatarRenderState renderState = new AvatarRenderState();

    public HairRenderLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer, HairModel model) {
        super(renderer, model);
        this.model = model;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>((test) -> {
            if (renderState.walkAnimationSpeed > 0) {
                test.setControllerSpeed(renderState.walkAnimationSpeed);
                return test.setAndContinue(DefaultAnimations.WALK);
            }
            return PlayState.STOP;
        }));
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
        return poseStack;
    }
}
