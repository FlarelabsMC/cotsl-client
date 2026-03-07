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

import java.util.UUID;

public class PlayerEyebrowRenderLayer<S extends AvatarRenderState, M extends PlayerModel> extends RenderLayer<S, M> {
    private final PlayerEyebrowModel eyebrowModel;

    public PlayerEyebrowRenderLayer(RenderLayerParent<S, M> renderer, EntityModelSet set) {
        super(renderer);
        this.eyebrowModel = new PlayerEyebrowModel(set.bakeLayer(PlayerEyebrowModel.MODEL_LAYER));
    }

    @Override
    public void submit(PoseStack stack, SubmitNodeCollector collector, int packedLight, S state, float yRot, float xRot) {
        stack.pushPose();
        this.getParentModel().head.translateAndRotate(stack);
        UUID uuid = ((AvatarRenderStateExt) state).getUUID();
        collector.submitModel(eyebrowModel, state, stack, RenderTypes.entityTranslucent(Identifier.parse("cotsl:avatars/" + uuid)), packedLight, OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor, null);
        stack.popPose();
    }

    public static class PlayerEyebrowModel extends EntityModel<AvatarRenderState> {
        public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(Identifier.parse("cotsl:eyebrows"), "main");
        private final ModelPart leftEyebrow, rightEyebrow;

        public PlayerEyebrowModel(ModelPart root) {
            super(root);
            this.leftEyebrow = root.getChild("left_eyebrow");
            this.rightEyebrow = root.getChild("right_eyebrow");
        }

        public static LayerDefinition createLayer() {
            MeshDefinition def = new MeshDefinition();
            PartDefinition head = def.getRoot();

            head.addOrReplaceChild(
                    "left_eyebrow",
                    CubeListBuilder.create()
                            .texOffs(1, 2)
                            .addBox(-3F, -6.25F, -4.02F, 2.0F, 1.0F, 0.0F),
                    PartPose.ZERO
            );

            head.addOrReplaceChild(
                    "right_eyebrow",
                    CubeListBuilder.create()
                            .texOffs(5, 2)
                            .addBox(1F, -6.25F, -4.02F, 2.0F, 1.0F, 0.0F),
                    PartPose.ZERO
            );

            return LayerDefinition.create(def, 64, 64);
        }

        @Override
        public void setupAnim(AvatarRenderState state) {
            boolean canBlink = state.ageInTicks % 100 < ((state.ageInTicks / 100) % 3 * 2);
            if (canBlink) {
                leftEyebrow.y = 1.25F;
                rightEyebrow.y = 1.25F;
            } else {
                leftEyebrow.y = 0f;
                rightEyebrow.y = 0f;
            }
        }
    }
}
