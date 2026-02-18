package com.flarelabsmc.cotsl.client.render.skin.layers;

import com.flarelabsmc.cotsl.client.render.skin.AvatarRenderStateExt;
import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.util.Mth;

import java.util.UUID;

public class PlayerEyeRenderLayer<S extends AvatarRenderState, M extends PlayerModel> extends RenderLayer<S, M> {
    private final PlayerEyeModel eyeModel;

    public PlayerEyeRenderLayer(RenderLayerParent<S, M> renderer, EntityModelSet set) {
        super(renderer);
        this.eyeModel = new PlayerEyeModel(set.bakeLayer(PlayerEyeModel.MODEL_LAYER));
    }

    @Override
    public void submit(PoseStack stack, SubmitNodeCollector collector, int packedLight, S state, float yRot, float xRot) {
        float xrot = state.xRot, yrot = state.yRot;
        stack.pushPose();
        this.getParentModel().head.translateAndRotate(stack);
        UUID uuid = ((AvatarRenderStateExt) state).getUUID();
        collector.submitModel(eyeModel, state, stack, RenderTypes.entityTranslucent(Identifier.parse("cotsl:avatars/" + uuid)), packedLight, OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor, null);
        stack.popPose();
    }

    public static class PlayerEyeModel extends EntityModel<AvatarRenderState> {
        public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(Identifier.parse("cotsl:eyes"), "main");
        private final ModelPart leftEye, rightEye;

        public PlayerEyeModel(ModelPart root) {
            super(root);
            this.leftEye = root.getChild("left_eye");
            this.rightEye = root.getChild("right_eye");
        }

        public static LayerDefinition createLayer() {
            MeshDefinition def = new MeshDefinition();
            PartDefinition head = def.getRoot();

            head.addOrReplaceChild(
                    "left_eye",
                    CubeListBuilder.create()
                            .texOffs(2, 3)
                            .addBox(-2.5F, -5.0F, -4.01F, 1.0F, 1.0F, 0.0F),
                    PartPose.ZERO
            );

            head.addOrReplaceChild(
                    "right_eye",
                    CubeListBuilder.create()
                            .texOffs(5, 3)
                            .addBox(1.5F, -5.0F, -4.01F, 1.0F, 1.0F, 0.0F),
                    PartPose.ZERO
            );

            return LayerDefinition.create(def, 64, 64);
        }

        @Override
        public void setupAnim(AvatarRenderState state) {
            float diff = ((Mth.wrapDegrees(state.bodyRot) * Mth.DEG_TO_RAD) - state.yRot) * 3f;
            this.leftEye.x = diff / 360;
            this.rightEye.x = diff / 360;
        }
    }
}
