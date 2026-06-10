package com.flarelabsmc.cotsl.core.transform.mixin.client;

import com.flarelabsmc.cotsl.client.CotSLClient;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(DebugScreenEntryList.class)
public class DebugScreenEntryListMixin {
    @Shadow
    @Final
    private Map<Identifier, DebugScreenEntryStatus> allStatuses;

    @Inject(method = "rebuildCurrentList", at = @At("HEAD"))
    private void resetToProfile(CallbackInfo ci) {
        Identifier setting = CotSLClient.PLATFORM_DEBUG_ENTRY;
        if (!this.allStatuses.containsKey(setting)) {
            this.allStatuses.put(setting, DebugScreenEntryStatus.IN_OVERLAY);
        }
    }
}
