package com.flarelabsmc.cotsl.client.render.skin.layers;

import com.flarelabsmc.cotsl.client.render.geo.layer.vanilla.GeoModelRenderLayer;
import com.flarelabsmc.cotsl.client.render.skin.layers.model.HairModel;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.util.GeckoLibUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public class HairRenderLayer extends GeoModelRenderLayer<AvatarRenderState, PlayerModel> {
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    public final HairModel model;
    private final UUID player;

    public HairRenderLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer, HairModel model, UUID player) {
        super(renderer, model);
        this.model = model;
        this.player = player;
    }

    public UUID getPlayer() {
        return player;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public PoseStack modifyPose(PoseStack poseStack, AvatarRenderState renderState, float yRot, float xRot) {
        this.getParentModel().head.translateAndRotate(poseStack);
        poseStack.translate(0.5, 0.5, -0.5);
        poseStack.mulPose(Axis.XP.rotationDegrees(180f));
        poseStack.mulPose(Axis.YP.rotationDegrees(180f));
        return poseStack;
    }
}
