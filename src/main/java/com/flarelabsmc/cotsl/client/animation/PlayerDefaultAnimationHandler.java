package com.flarelabsmc.cotsl.client.animation;

import com.flarelabsmc.cotsl.common.CotSL;
import com.flarelabsmc.cotsl.core.transform.duck.AbstractHorseDuck;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.animation.PlayerRawAnimationBuilder;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranimcore.animation.AnimationController;
import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractFadeModifier;
import com.zigythebird.playeranimcore.bones.AdvancedPlayerAnimBone;
import com.zigythebird.playeranimcore.easing.EasingType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import static com.zigythebird.playeranim.PlayerAnimLibMod.ANIMATION_LAYER_ID;

@EventBusSubscriber(Dist.CLIENT)
public class PlayerDefaultAnimationHandler {
    public static void init() {}

    @SubscribeEvent
    public static void handle(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.player instanceof LocalPlayer player)) return;
        PlayerAnimationController controller = (PlayerAnimationController) PlayerAnimationAccess.getPlayerAnimationLayer(player, ANIMATION_LAYER_ID);
        if (controller == null) return;
        Avatar a = controller.getAvatar();
        if (a.getVehicle() instanceof AbstractHorse horse) {
            AbstractHorseDuck hd = (AbstractHorseDuck) horse;
            if (hd.getVel() > 0) {
                Identifier horseAnim = Identifier.parse("cotsl:horse_ride");
                if (!controller.isPlayingTriggeredAnimation()) {
                    controller.triggerAnimation(horseAnim);
                    controller.addModifierBefore(AbstractFadeModifier.standardFadeIn(10, EasingType.EASE_OUT_BACK));
                    controller.setPostAnimationSetupConsumer(bonefunc -> {
                        AdvancedPlayerAnimBone head = bonefunc.apply("head");
                        head.rotYEnabled = false;
                    });
                }
                return;
            }
        }
        controller.stopTriggeredAnimation();
    }
}
