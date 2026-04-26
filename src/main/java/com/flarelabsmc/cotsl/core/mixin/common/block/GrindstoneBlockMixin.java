package com.flarelabsmc.cotsl.core.mixin.common.block;

import com.flarelabsmc.cotsl.client.particle.options.FireSparkParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GrindstoneBlock.class)
public class GrindstoneBlockMixin {
    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        for (int i = 0; i < 100; i++) {
            double fv = 0.04 + level.getRandom().nextDouble() * 0.04;
            if (hitResult instanceof BlockHitResult result) level.addParticle(
                    new FireSparkParticleOptions(
                            40 + level.getRandom().nextInt(80),
                            4f
                    ),
                    result.getLocation().x, result.getLocation().y, result.getLocation().z, fv, fv, fv
            );
        }
        level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0f, 1.0f, false);
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
