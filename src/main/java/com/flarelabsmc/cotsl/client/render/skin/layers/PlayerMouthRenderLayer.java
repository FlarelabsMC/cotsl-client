package com.flarelabsmc.cotsl.client.render.skin.layers;

import com.flarelabsmc.cotsl.client.render.skin.AvatarRenderStateExt;
import com.flarelabsmc.cotsl.client.speech.SpeechData;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class PlayerMouthRenderLayer<S extends AvatarRenderState, M extends PlayerModel> extends RenderLayer<S, M> {
    private final PlayerMouthModel mouthModel;

    public PlayerMouthRenderLayer(RenderLayerParent<S, M> renderer, EntityModelSet set) {
        super(renderer);
        this.mouthModel = new PlayerMouthModel(set.bakeLayer(PlayerMouthModel.MODEL_LAYER));
    }

    @Override
    public void submit(PoseStack stack, SubmitNodeCollector collector, int packedLight, S state, float yRot, float xRot) {
        stack.pushPose();
        this.getParentModel().head.translateAndRotate(stack);
        AvatarRenderStateExt ext = (AvatarRenderStateExt) state;
        ext.setCurrentSpeech("test");
        float duration = SpeechData.getDuration("test");
        ext.setSpeechProgress((float) (System.currentTimeMillis() / 1000.0 % duration));

        String speech = ext.getCurrentSpeech();
        float progress = ext.getSpeechProgress();
        int pose = speech != null ? SpeechData.getMouthPoseAtTime(speech, progress) : 8;
        ext.setMouthPose(pose);
        RenderType type = RenderTypes.entityTranslucent(Identifier.parse("cotsl:textures/skin/mouth_poses.png"));
        collector.submitModelPart(mouthModel.mouthPoses[pose], stack, type, packedLight, OverlayTexture.NO_OVERLAY, null);

        stack.popPose();
    }

    public static class PlayerMouthModel extends EntityModel<AvatarRenderState> {
        public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(Identifier.parse("cotsl:mouth"), "main");
        ModelPart[] mouthPoses;

        public PlayerMouthModel(ModelPart root) {
            super(root);

            this.mouthPoses = new ModelPart[9];
            for (int i = 0; i < 9; i++) {
                this.mouthPoses[i] = root.getChild("mouth_" + i);
            }
        }

        public static LayerDefinition createLayer() {
            MeshDefinition def = new MeshDefinition();
            PartDefinition root = def.getRoot();
            for (int i = 0; i < 9; i++) {
                root.addOrReplaceChild(
                        "mouth_" + i,
                        CubeListBuilder.create()
                                .texOffs( i * 7 + 1, 1)
                                .addBox(-1.5F, -3.0F, -4.01F, 3.0F, 3.0F, 0.0F),
                        PartPose.ZERO
                );
            }
            return LayerDefinition.create(def, 64, 4);
        }

        @Override
        public void setupAnim(AvatarRenderState state) {
            super.setupAnim(state);
            AvatarRenderStateExt ext = (AvatarRenderStateExt) state;
            for (ModelPart mouthPose : mouthPoses) mouthPose.visible = false;
            mouthPoses[ext.getMouthPose()].visible = true;
        }
    }
}