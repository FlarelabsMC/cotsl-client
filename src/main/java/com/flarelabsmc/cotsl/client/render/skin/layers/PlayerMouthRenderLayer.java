package com.flarelabsmc.cotsl.client.render.skin.layers;

import com.flarelabsmc.cotsl.client.render.skin.AvatarRenderStateExt;
import com.flarelabsmc.cotsl.client.speech.SpeechData;
import com.flarelabsmc.cotsl.common.network.NetworkHandler;
import com.flarelabsmc.cotsl.common.sound.CotSLSoundEvents;
import com.flarelabsmc.cotsl.common.sound.TrackableSoundInstance;
import com.flarelabsmc.cotsl.common.storage.user.PermanentUser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.DebugStickItem;

public class PlayerMouthRenderLayer<S extends AvatarRenderState, M extends PlayerModel> extends RenderLayer<S, M> {
    private final PlayerMouthModel mouthModel;
    private TrackableSoundInstance currentSound;

    public PlayerMouthRenderLayer(RenderLayerParent<S, M> renderer, EntityModelSet set) {
        super(renderer);
        this.mouthModel = new PlayerMouthModel(set.bakeLayer(PlayerMouthModel.MODEL_LAYER));
    }

    @Override
    public void submit(PoseStack stack, SubmitNodeCollector collector, int packedLight, S state, float yRot, float xRot) {
        stack.pushPose();
        this.getParentModel().head.translateAndRotate(stack);
        AvatarRenderStateExt ext = (AvatarRenderStateExt) state;

        if (currentSound == null) {
            if (Minecraft.getInstance().player.getMainHandItem().getItem() instanceof DebugStickItem) {
                TrackableSoundInstance sound = new TrackableSoundInstance(
                        CotSLSoundEvents.TEST_2,
                        SoundSource.VOICE,
                        Minecraft.getInstance().player, 0, 0, 0
                );
                Minecraft.getInstance().getSoundManager().play(sound);
                currentSound = sound;
            }
        }
        int pose = 0;
        if (currentSound != null) {
            float soundProgress = currentSound.getProgress();
            pose = SpeechData.getMouthPoseAtTime("test_2", soundProgress);
            ext.setMouthPose(pose);
            if (currentSound.isStopped()) {
                currentSound = null;
                ext.setMouthPose(0);
            }
        }
        PermanentUser user = NetworkHandler.getCachedUserData(ext.getUUID());
        if (user == null) {
            stack.popPose();
            return;
        }

        stack.mulPose(Axis.ZP.rotationDegrees(2.5f));

        RenderType type = RenderTypes.entityTranslucent(Identifier.parse("cotsl:textures/skin/mouth/mouth_" + user.getCharacterData().skinColor() + ".png"));
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
            AvatarRenderStateExt ext = (AvatarRenderStateExt) state;
            for (ModelPart mouthPose : mouthPoses) mouthPose.visible = false;
            mouthPoses[ext.getMouthPose()].visible = true;
        }
    }
}