package com.flarelabsmc.cotsl.core.mixin.client.skin;

import com.flarelabsmc.cotsl.client.render.skin.AvatarRenderStateExt;
import com.flarelabsmc.cotsl.client.render.skin.layers.HairRenderLayer;
import com.flarelabsmc.cotsl.client.render.skin.layers.PlayerEyeRenderLayer;
import com.flarelabsmc.cotsl.client.render.skin.layers.PlayerMouthRenderLayer;
import com.flarelabsmc.cotsl.client.render.skin.layers.model.HairModel;
import com.flarelabsmc.cotsl.client.render.texture.CharacterSkinGenerator;
import com.flarelabsmc.cotsl.client.render.texture.Frankenstein;
import com.flarelabsmc.cotsl.common.network.NetworkHandler;
import com.flarelabsmc.cotsl.common.storage.player.CharData;
import com.flarelabsmc.cotsl.common.storage.user.PermanentUser;
import com.flarelabsmc.cotsl.common.storage.user.PermanentUserHandler;
import com.flarelabsmc.cotsl.common.storage.user.PermanentUserStorage;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.sql.SQLException;
import java.util.UUID;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin<AvatarlikeEntity extends Avatar & ClientAvatarEntity> extends LivingEntityRenderer<AvatarlikeEntity, AvatarRenderState, PlayerModel> {
    private HairRenderLayer hairLayer;
    private Identifier hairTexture = Identifier.fromNamespaceAndPath("cotsl", "skin/hair/hair_0");
    private Identifier hairModel = Identifier.fromNamespaceAndPath("cotsl", "skin/hair/hair_0.png");
    private UUID playerUUID = UUID.randomUUID();

    public AvatarRendererMixin(EntityRendererProvider.Context context, PlayerModel model, float shadowRadius, AvatarRenderState state) {
        super(context, model, shadowRadius);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        HairRenderLayer hairLayer = new HairRenderLayer((AvatarRenderer<?>) (Object) this, new HairModel(hairTexture, hairModel), playerUUID);
        this.addLayer(new PlayerEyeRenderLayer<>((AvatarRenderer<?>) (Object) this, context.getModelSet()));
        this.addLayer(new PlayerMouthRenderLayer<>((AvatarRenderer<?>) (Object) this, context.getModelSet()));
        this.addLayer(hairLayer);
        this.hairLayer = hairLayer;
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRendererProvider$Context;bakeLayer(Lnet/minecraft/client/model/geom/ModelLayerLocation;)Lnet/minecraft/client/model/geom/ModelPart;"))
    private static ModelPart setSlim(EntityRendererProvider.Context instance, ModelLayerLocation layer) {
        return instance.bakeLayer(ModelLayers.PLAYER_SLIM);
    }

    @Inject(method = "extractRenderState*", at = @At("TAIL"))
    private void storeUUID(AvatarlikeEntity entity, AvatarRenderState state, float partialTick, CallbackInfo ci) throws SQLException {
        playerUUID = entity.getUUID();
        ((AvatarRenderStateExt) state).setUUID(playerUUID);
        Identifier id = Identifier.parse("cotsl:avatars/" + entity.getUUID());
        DynamicTexture cached = Frankenstein.getCachedTexture(id);
        if (cached != null) return;
        PermanentUser user = NetworkHandler.getCachedUserData(entity.getUUID());
        CharData data = user.getCharacterData();
//        NativeImage skin = CharacterSkinGenerator.createSkin(CharData.init().rebuild().gender(1).bodyType(2).shirtColor(0x435241).pantsColor(0xc4ba86).headShape(2).jawShape(2).build());
        CharData newData = CharData.init().rebuild().bodyType(2).shirtColor(0x435241).pantsColor(0xc4ba86).headShape(3).jawShape(0).eyesColor(1).build();
        NativeImage skin = CharacterSkinGenerator.createSkin(newData);
        user.setCharacterData(newData);
        hairTexture = Identifier.fromNamespaceAndPath("cotsl", "textures/skin/hair/hair_" + data.hair() + ".png");
        hairModel = Identifier.fromNamespaceAndPath("cotsl", "skin/hair/hair_" + data.hair());
        hairLayer.model.setStyle(newData.hair());
        hairLayer.model.setTexture(hairTexture);
        hairLayer.model.setModel(hairModel);
        Frankenstein.registerTexture(id, skin);
    }

    @Inject(method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Lnet/minecraft/resources/Identifier;", at = @At("RETURN"), cancellable = true)
    private void getTextureLocation(AvatarRenderState state, CallbackInfoReturnable<Identifier> cir) {
        cir.setReturnValue(Identifier.parse("cotsl:avatars/" + ((AvatarRenderStateExt) state).getUUID()));
    }
}
