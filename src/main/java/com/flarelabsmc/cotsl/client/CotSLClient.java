package com.flarelabsmc.cotsl.client;

import com.flarelabsmc.cotsl.client.render.EntityRenderers;
import com.flarelabsmc.cotsl.common.CotSL;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = CotSL.MOD_ID, dist = Dist.CLIENT)
public class CotSLClient {
    public CotSLClient(ModContainer container) {
        EntityRenderers.init();
    }
}
