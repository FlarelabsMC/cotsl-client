package com.flarelabsmc.cotsl.core.mixin.client.entity.avatar;

import com.flarelabsmc.cotsl.client.render.skin.AvatarRenderStateExt;
import com.flarelabsmc.cotsl.client.render.skin.layers.*;
import com.flarelabsmc.cotsl.client.render.skin.layers.model.HairModel;
import com.flarelabsmc.cotsl.common.network.NetworkHandler;
import com.flarelabsmc.cotsl.common.network.packets.RequestUserDataPacket;
import com.flarelabsmc.cotsl.core.hotmixin.MixinsClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.Connection;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin<AvatarlikeEntity extends Avatar & ClientAvatarEntity> extends LivingEntityRenderer<AvatarlikeEntity, AvatarRenderState, PlayerModel> {
    private PlayerHairRenderLayer hairLayer;
    private Identifier hairTexture = Identifier.fromNamespaceAndPath("cotsl", "skin/hair/hair_0");
    private Identifier hairModel = Identifier.fromNamespaceAndPath("cotsl", "skin/hair/hair_0");
    private UUID playerUUID = UUID.randomUUID();
    private final MixinsClient.AvatarRendererMixin<AvatarlikeEntity> self = new MixinsClient.AvatarRendererMixin<>();

    public AvatarRendererMixin(EntityRendererProvider.Context context, PlayerModel model, float shadowRadius, AvatarRenderState state) {
        super(context, model, shadowRadius);
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void submit(AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
        UUID uuid = ((AvatarRenderStateExt) state).getUUID();
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        if (listener == null) return;
        Connection connection = listener.getConnection();
        if (!connection.isConnected()) return;
        if (NetworkHandler.getCachedUserData(uuid) == null && NetworkHandler.tryAddPendingRequest(uuid)) {
            Minecraft.getInstance().execute(() ->
                    ClientPacketDistributor.sendToServer(new RequestUserDataPacket(uuid))
            );
        }
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"))
    private void submit2(AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
        self.submit(state, poseStack, submitNodeCollector, camera, ci);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        PlayerHairRenderLayer hairLayer = new PlayerHairRenderLayer((AvatarRenderer<?>) (Object) this, new HairModel(hairTexture, hairModel));
        PlayerEyebrowRenderLayer<AvatarRenderState, PlayerModel> eyebrowLayer = new PlayerEyebrowRenderLayer<>((AvatarRenderer<?>) (Object) this, context.getModelSet());
        this.addLayer(new PlayerEyeRenderLayer<>((AvatarRenderer<?>) (Object) this, context.getModelSet()));
        this.addLayer(new PlayerMouthRenderLayer<>((AvatarRenderer<?>) (Object) this, context.getModelSet()));
        this.addLayer(hairLayer);
        this.addLayer(eyebrowLayer);
        this.hairLayer = hairLayer;
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRendererProvider$Context;bakeLayer(Lnet/minecraft/client/model/geom/ModelLayerLocation;)Lnet/minecraft/client/model/geom/ModelPart;"))
    private static ModelPart setSlim(EntityRendererProvider.Context instance, ModelLayerLocation layer) {
        return instance.bakeLayer(ModelLayers.PLAYER_SLIM);
    }

    @Inject(method = "extractRenderState*", at = @At("TAIL"))
    private void storeUUID(AvatarlikeEntity entity, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        self.extractRenderState(entity, state, partialTick, ci, playerUUID, hairLayer);
    }

    @Inject(method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Lnet/minecraft/resources/Identifier;", at = @At("RETURN"), cancellable = true)
    private void getTextureLocation(AvatarRenderState state, CallbackInfoReturnable<Identifier> cir) {
        cir.setReturnValue(Identifier.parse("cotsl:avatars/" + ((AvatarRenderStateExt) state).getUUID()));
    }
}
