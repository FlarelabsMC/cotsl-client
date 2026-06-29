package com.flarelabsmc.cotsl.client.animation;

import com.flarelabsmc.cotsl.core.transform.duck.AvatarDuck;
import com.zigythebird.playeranim.animation.PlayerAnimResources;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranimcore.animation.Animation;
import com.zigythebird.playeranimcore.animation.RawAnimation;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractFadeModifier;
import com.zigythebird.playeranimcore.animation.layered.modifier.SpeedModifier;
import com.zigythebird.playeranimcore.bones.AdvancedPlayerAnimBone;
import com.zigythebird.playeranimcore.easing.EasingType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import static com.zigythebird.playeranim.PlayerAnimLibMod.ANIMATION_LAYER_ID;

@EventBusSubscriber(Dist.CLIENT)
public class PlayerDefaultAnimationHandler {
    public static void init() {}

    @SubscribeEvent
    public static void handle(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        PlayerAnimationController controller =
                (PlayerAnimationController) PlayerAnimationAccess.getPlayerAnimationLayer(
                        player, ANIMATION_LAYER_ID
                );
        if (controller == null) return;
        Avatar avatar = controller.getAvatar();
        AvatarDuck avatarExt = (AvatarDuck) avatar;
        boolean wasMoving = avatarExt.wasMoving();
        if (avatar.getVehicle() instanceof AbstractHorse horse) {
            float speed = (float) horse.getDeltaMovement().normalize().length();
            boolean isMoving = speed > 0;
            String startMoving = "cotsl:horse_start_moving";
            String idle = "cotsl:horse_idle";
            if (isMoving && !wasMoving) {
                fadeAnimation(getRawAnim(startMoving, false), EasingType.LINEAR, controller, speed);
                avatarExt.setWasMoving(true);
            } else if (isMoving) {
                String horseAnim = speed > 0.1 ? "cotsl:horse_run" : "cotsl:horse_walk";
                if (isPlayingAnim(startMoving, controller)) return;
                fadeAnimation(getRawAnim(horseAnim, true), EasingType.LINEAR, controller, speed);
            } else {
                if (!isPlayingAnim(idle, controller)) fadeAnimation(getRawAnim(idle, true), EasingType.LINEAR, controller, speed);
                avatarExt.setWasMoving(false);
            }
            return;
        }
        controller.stopTriggeredAnimation();
    }

    private static boolean isPlayingAnim(String anim, PlayerAnimationController controller) {
        return controller.getCurrentAnimation() != null && controller.getCurrentAnimation().animation() == getAnim(anim);
    }

    private static Animation getAnim(String anim) {
        return PlayerAnimResources.getAnimation(Identifier.parse(anim));
    }

    private static RawAnimation getRawAnim(String anim, boolean loop) {
        Animation animation = PlayerAnimResources.getAnimation(Identifier.parse(anim));
        RawAnimation init = RawAnimation.begin();
        return loop ? init.thenLoop(animation) : init.thenPlay(animation);
    }

    private static void fadeAnimation(RawAnimation anim, EasingType easing, PlayerAnimationController controller, float speed) {
        controller.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(10, easing), anim);
        if (!controller.getModifier(0).isActive()) controller.addModifier(new SpeedModifier(speed), 0);
    }
}
