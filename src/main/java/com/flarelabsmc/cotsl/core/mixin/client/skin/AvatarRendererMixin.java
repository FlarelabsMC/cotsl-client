package com.flarelabsmc.cotsl.core.mixin.client.skin;

import com.flarelabsmc.cotsl.client.render.skin.AvatarRenderStateExt;
import com.flarelabsmc.cotsl.client.render.skin.layers.PlayerEyeRenderLayer;
import com.flarelabsmc.cotsl.client.render.texture.CharacterSkinGenerator;
import com.flarelabsmc.cotsl.client.render.texture.Frankenstein;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.entity.ClientAvatarEntity;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin<AvatarlikeEntity extends Avatar & ClientAvatarEntity> extends LivingEntityRenderer<AvatarlikeEntity, AvatarRenderState, PlayerModel> {
    private final AvatarRenderState state;

    public AvatarRendererMixin(EntityRendererProvider.Context context, PlayerModel model, float shadowRadius, AvatarRenderState state) {
        super(context, model, shadowRadius);
        this.state = state;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        this.addLayer(new PlayerEyeRenderLayer<>((AvatarRenderer<?>) (Object) this, context.getModelSet()));
    }

    @Inject(method = "extractRenderState*", at = @At("TAIL"))
    private void storeUUID(AvatarlikeEntity entity, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        ((AvatarRenderStateExt) state).setUUID(entity.getUUID());
        Identifier id = Identifier.parse("cotsl:avatars/" + entity.getUUID());
        DynamicTexture cached = Frankenstein.getCachedTexture(id);
        if (cached != null) {
            return;
        }
        NativeImage skin = CharacterSkinGenerator.createSkin(0, 0, 0, 0, 0, 0, 0, 0, 0);
        Frankenstein.registerTexture(id, skin);
    }

    @Inject(method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Lnet/minecraft/resources/Identifier;", at = @At("RETURN"), cancellable = true)
    private void getTextureLocation(AvatarRenderState state, CallbackInfoReturnable<Identifier> cir) {
        cir.setReturnValue(Identifier.parse("cotsl:avatars/" + ((AvatarRenderStateExt) state).getUUID()));
    }
}
