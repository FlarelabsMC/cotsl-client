package com.flarelabsmc.cotsl.client.render.skin.layers;

import com.flarelabsmc.cotsl.client.render.skin.AvatarRenderStateExt;
import com.flarelabsmc.cotsl.common.network.NetworkHandler;
import com.flarelabsmc.cotsl.common.storage.player.CharData;
import com.flarelabsmc.cotsl.common.storage.user.PermanentUser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public class PlayerHandsRenderLayer<S extends AvatarRenderState, M extends PlayerModel> extends RenderLayer<S, M> {
    private final LeftHandModel leftHandModel;
    private final RightHandModel rightHandModel;

    public PlayerHandsRenderLayer(RenderLayerParent<S, M> renderer, EntityModelSet set) {
        super(renderer);
        this.leftHandModel = new LeftHandModel(set.bakeLayer(LeftHandModel.MODEL_LAYER));
        this.rightHandModel = new RightHandModel(set.bakeLayer(RightHandModel.MODEL_LAYER));
    }

    @Override
    public void submit(PoseStack stack, SubmitNodeCollector collector, int packedLight, S state, float yRot, float xRot) {
        UUID uuid = ((AvatarRenderStateExt) state).getUUID();
        PermanentUser user = NetworkHandler.getCachedUserData(uuid);
        if (user == null) return;
        CharData charData = user.getCharacterData();
        int skinColor = charData.skinColor();
        stack.pushPose();
        this.getParentModel().leftArm.translateAndRotate(stack);
        stack.rotateAround(Axis.ZP.rotationDegrees(-22.5f), 0F, 1, 1F);
        stack.translate(0.135, 0.07, 0);
        stack.scale(1.01f, 1.01f, 1.01f);
        collector.submitModel(leftHandModel, state, stack, RenderTypes.entityTranslucent(Identifier.parse("cotsl:textures/skin/hands/hands_" + skinColor + ".png")), packedLight, OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor, null);
        stack.popPose();
        stack.pushPose();
        this.getParentModel().rightArm.translateAndRotate(stack);
        stack.rotateAround(Axis.ZP.rotationDegrees(22.5f), 0F, 1, 1f);
        stack.translate(-0.135, 0.07, 0);
        stack.scale(1.01f, 1.01f, 1.01f);
        collector.submitModel(rightHandModel, state, stack, RenderTypes.entityTranslucent(Identifier.parse("cotsl:textures/skin/hands/hands_" + skinColor + ".png")), packedLight, OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor, null);
        stack.popPose();
    }

    public static class LeftHandModel extends EntityModel<AvatarRenderState> {
        public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(Identifier.parse("cotsl:left_hand"), "main");
        private final ModelPart leftHand;

        public LeftHandModel(ModelPart root) {
            super(root);
            this.leftHand = root.getChild("left_hand");
        }

        public static LayerDefinition createLayer() {
            MeshDefinition def = new MeshDefinition();
            PartDefinition head = def.getRoot();

            head.addOrReplaceChild(
                    "left_hand",
                    CubeListBuilder.create()
                            .texOffs(0, 0)
                            .addBox(0F, 8, -2F, 2.0F, 2.0F, 4.0F),
                    PartPose.ZERO
            );

            return LayerDefinition.create(def, 12, 12);
        }

        @Override
        public void setupAnim(AvatarRenderState state) {
        }
    }

    public static class RightHandModel extends EntityModel<AvatarRenderState> {
        public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(Identifier.parse("cotsl:right_hand"), "main");
        private final ModelPart rightHand;

        public RightHandModel(ModelPart root) {
            super(root);
            this.rightHand = root.getChild("right_hand");
        }

        public static LayerDefinition createLayer() {
            MeshDefinition def = new MeshDefinition();
            PartDefinition head = def.getRoot();

            head.addOrReplaceChild(
                    "right_hand",
                    CubeListBuilder.create()
                            .texOffs(0, 6)
                            .mirror()
                            .addBox(-2F, 8, -2F, 2.0F, 2.0F, 4.0F),
                    PartPose.ZERO
            );

            return LayerDefinition.create(def, 12, 12);
        }

        @Override
        public void setupAnim(AvatarRenderState state) {
        }
    }
}
