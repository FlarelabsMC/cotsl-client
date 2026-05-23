package com.flarelabsmc.cotsl.client.entity.replaced;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.GeoReplacedEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.constant.DataTickets;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.world.phys.Vec3;

public class ReplacedHorse implements GeoReplacedEntity {
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private boolean wasMoving;

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("horse_controller", 0, state -> {
            Vec3 movement = state.getData(DataTickets.VELOCITY);
            if (movement == null) return PlayState.CONTINUE;
            float vel = state.isMoving() ? (float) movement.normalize().length() : 1;
            state.setControllerSpeed(vel);
            String startMoving = "animation.horse.start_moving";
            if (state.isMoving() && !wasMoving) {
                wasMoving = true;
                return state.setAndContinue(RawAnimation.begin().thenPlay(startMoving));
            } else if (state.isMoving()) {
                if (isPlayingAnim(startMoving, state)) return PlayState.CONTINUE;
                wasMoving = true;
                return state.setAndContinue(RawAnimation.begin().thenPlay(vel > 0.05 ? "animation.horse.run" : "animation.horse.walk"));
            } else {
                wasMoving = false;
                return state.setAndContinue(RawAnimation.begin().thenPlay("animation.horse.idle"));
            }
        }).setTransitionTicks(10));
    }

    private static boolean isPlayingAnim(String anim, AnimationTest<GeoAnimatable> state) {
        return state.controller().isTriggeredAnimation(anim);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
