package com.flarelabsmc.cotsl.core.transform.mixin.common.entity.living;

import com.flarelabsmc.cotsl.core.transform.MixinsCommon;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// based on the mod Immersive Horse Riding (MIT licensed), but I wanted to implement it my own way
@Mixin(AbstractHorse.class)
public abstract class AbstractHorseMixin extends Animal {
    @Shadow private float standAnimO;
    @Unique private final MixinsCommon.AbstractHorseMixin self = new MixinsCommon.AbstractHorseMixin();
    @Unique private float angularVelocity = 0.0f;
    @Unique private float speed = 0.0f;

    protected AbstractHorseMixin(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public void setSpeed(float speed) {
        this.speed = speed;
    }

    @Inject(method = "getRiddenRotation", at = @At("HEAD"), cancellable = true)
    private void getRiddenRotation(LivingEntity controller, CallbackInfoReturnable<Vec2> cir) {
        self.getRiddenRotation((AbstractHorse) (Object) this, controller, cir);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        self.tick((AbstractHorse) (Object) this, ci);
    }

    @ModifyReturnValue(method = "getRiddenInput", at = @At("RETURN"))
    private Vec3 getRiddenInput(Vec3 original, Player player, Vec3 selfInput) {
        return self.getRiddenInput((AbstractHorse) (Object) this, original, player, selfInput);
    }

    @Inject(method = "positionRider", at = @At("TAIL"))
    protected void positionRider(Entity passenger, MoveFunction moveFunction, CallbackInfo ci) {
        self.positionRider((AbstractHorse) (Object) this, passenger, moveFunction);
    }

    @Inject(method = "getPassengerAttachmentPoint", at = @At("RETURN"), cancellable = true)
    private void getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale, CallbackInfoReturnable<Vec3> cir) {
        self.getPassengerAttachmentPoint((AbstractHorse) (Object) this, standAnimO, passenger, dimensions, scale, cir);
    }
}