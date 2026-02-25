package com.flarelabsmc.cotsl.common.sound;

import com.flarelabsmc.cotsl.common.CotSL;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber
public class CotSLSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, CotSL.MOD_ID);

    public static final Holder<SoundEvent> TEST = SOUND_EVENTS.register("test", (i) -> SoundEvent.createFixedRangeEvent(i, 16f));
}
