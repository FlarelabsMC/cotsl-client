package com.flarelabsmc.cotsl.common.entity.replaced;

import com.geckolib.animatable.GeoReplacedEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.util.Mth;

public class ReplacedHorse implements GeoReplacedEntity {
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private boolean wasMoving;
    private float speed;

    public void setCurrentSpeed(float speed) {
        this.speed = speed;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("horse_controller", 0, state -> {
            state.setControllerSpeed(speed);
            if (state.isMoving() && !wasMoving) {
                if (state.controller().hasAnimationFinished()) wasMoving = true;
                else return state.setAndContinue(RawAnimation.begin().thenPlay("animation.horse.start_moving"));
            }
            if (state.isMoving() && wasMoving)
                return state.setAndContinue(RawAnimation.begin().thenPlay(speed > 0.05 ? "animation.horse.run" : "animation.horse.walk"));
            // idle
            wasMoving = state.isMoving();
            speed = 1.0f;
            return state.setAndContinue(RawAnimation.begin().thenPlay("animation.horse.idle"));
        }).setTransitionTicks(10));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
