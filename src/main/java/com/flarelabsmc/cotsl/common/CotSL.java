package com.flarelabsmc.cotsl.common;

import com.flarelabsmc.cotsl.common.entity.EntityRegistry;
import com.flarelabsmc.cotsl.common.sound.CotSLSoundEvents;
import com.flarelabsmc.cotsl.common.storage.user.PermanentUserHandler;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(CotSL.MOD_ID)
public class CotSL {
    public static final String MOD_ID = "cotsl";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CotSL(IEventBus modEventBus, ModContainer modContainer) {
        PermanentUserHandler.init();
//        EntityRegistry.init();
        CotSLSoundEvents.SOUND_EVENTS.register(modEventBus);

//        EntityRegistry.ENTITY_TYPES.register(modEventBus);
    }
}
