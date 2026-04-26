package com.flarelabsmc.cotsl.core.hotmixin;

import com.flarelabsmc.cotsl.client.particle.options.FireSparkParticleOptions;
import com.flarelabsmc.cotsl.client.render.skin.AvatarRenderStateExt;
import com.flarelabsmc.cotsl.client.render.skin.layers.PlayerHairRenderLayer;
import com.flarelabsmc.cotsl.client.render.texture.CharacterSkinGenerator;
import com.flarelabsmc.cotsl.client.render.texture.Frankenstein;
import com.flarelabsmc.cotsl.common.network.NetworkHandler;
import com.flarelabsmc.cotsl.common.registry.ParticleRegistry;
import com.flarelabsmc.cotsl.common.storage.player.CharData;
import com.flarelabsmc.cotsl.common.storage.user.PermanentUser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

public final class MixinsClient {
    public static class EntityRenderDispatcherMixin {
        public void submit_submitFlame(PoseStack poseStack, EntityRenderState entityRenderState, Quaternionf quaternionf) {
            if (!entityRenderState.displayFireAnimation || Minecraft.getInstance().isPaused()) return;
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) return;
            RandomSource random = level.getRandom();
            float hw = entityRenderState.boundingBoxWidth * 0.5f;
            float height = entityRenderState.boundingBoxHeight;
            double x = entityRenderState.x;
            double y = entityRenderState.y;
            double z = entityRenderState.z;
            for (int i = 0; i < 10; i++) {
                double px = x + (random.nextDouble() * 2.0 - 1.0) * hw;
                double py = y + random.nextDouble() * height;
                double pz = z + (random.nextDouble() * 2.0 - 1.0) * hw;
                double fv = 0.04 + random.nextDouble() * 0.04;
                double vy = 0.04 + random.nextDouble() * 0.04;
                if (random.nextFloat() > 0.05f) return;
                level.addParticle(random.nextBoolean() ? ParticleRegistry.FIRE_FLAME.get() : ParticleTypes.LARGE_SMOKE, px, py, pz, 0.0, vy, 0.0);
            }
        }
    }

    public static class AvatarRendererMixin<AvatarlikeEntity extends Avatar> {
        public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {}

        public void extractRenderState(AvatarlikeEntity entity, AvatarRenderState state, float partialTick, CallbackInfo ci, UUID playerUUID, PlayerHairRenderLayer hairLayer) {
            playerUUID = entity.getUUID();
            ((AvatarRenderStateExt) state).setUUID(playerUUID);
            ((AvatarRenderStateExt) state).setHealth(entity.getHealth());
            ((AvatarRenderStateExt) state).setMaxHealth(entity.getMaxHealth());
            Identifier skinId = Identifier.parse("cotsl:avatars/" + entity.getUUID());
            Identifier hairId = Identifier.parse("cotsl:hair/" + entity.getUUID());
            if (!Frankenstein.isRegistered(skinId)) Frankenstein.registerPlaceholder(skinId);
            if (!Frankenstein.isRegistered(hairId)) Frankenstein.registerPlaceholder(hairId);
            if (!Frankenstein.isPlaceholder(skinId) && !Frankenstein.isPlaceholder(hairId)) return;
            PermanentUser user = NetworkHandler.getCachedUserData(entity.getUUID());
            if (user == null) return;
            CharData data = user.getCharacterData();

            if (Frankenstein.isPlaceholder(hairId)) {
                Identifier newHairTexture = Identifier.fromNamespaceAndPath("cotsl", "skin/hair/hair_" + data.hair() + "_color");
                Identifier newHairModel = Identifier.fromNamespaceAndPath("cotsl", "skin/hair/hair_" + data.hair());
                hairLayer.model.setStyle(data.hair());
                hairLayer.model.setModel(newHairModel);
                if (hairLayer.model.setTexture(newHairTexture)) Frankenstein.markLoaded(hairId);
            }
            if (Frankenstein.isPlaceholder(skinId)) {
                CharData newData = CharData.init().rebuild().bodyType(2).shirtColor(0x435241).pantsColor(0xc4ba86).headShape(3).jawShape(0).eyesColor(1).build();
                NativeImage skin = CharacterSkinGenerator.createSkin(newData);
                user.setCharacterData(newData);
                Frankenstein.updateTexture(skinId, skin);
            }
        }
    }
}
