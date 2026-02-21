package com.flarelabsmc.cotsl.core.mixin.client.skin;

import com.flarelabsmc.cotsl.client.render.CotSLEntityRenderers;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Function;

@Mixin(PlayerModel.class)
public class PlayerModelMixin extends HumanoidModel<AvatarRenderState> {
    public PlayerModelMixin(ModelPart root) {
        super(root);
    }

    @Unique
    private static RenderType getRenderType(Identifier texture) {
        //test type
        return RenderType.create("entity_cutout_no_cull_emissive", RenderSetup.builder(CotSLEntityRenderers.ENTITY_CUTOUT_NO_CULL_EMISSIVE)
                .withTexture("Sampler0", texture)
                .affectsCrumbling()
                .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                .createRenderSetup()
        );
    }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/HumanoidModel;<init>(Lnet/minecraft/client/model/geom/ModelPart;Ljava/util/function/Function;)V"))
    private static Function<Identifier, RenderType> modifyRenderType(Function<Identifier, RenderType> original) {
        return PlayerModelMixin::getRenderType;
    }
}
