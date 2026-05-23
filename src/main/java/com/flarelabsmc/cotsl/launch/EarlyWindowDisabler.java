package com.flarelabsmc.cotsl.launch;

import net.neoforged.fml.loading.FMLConfig;
import net.neoforged.neoforgespi.earlywindow.GraphicsBootstrapper;

public class EarlyWindowDisabler implements GraphicsBootstrapper {
    @Override
    public String name() {
        return "early_window_disabler";
    }

    @Override
    public void bootstrap(String[] arguments) {
        FMLConfig.updateConfig(FMLConfig.ConfigValue.EARLY_WINDOW_PROVIDER, "none");
        FMLConfig.updateConfig(FMLConfig.ConfigValue.EARLY_WINDOW_CONTROL, false);
    }
}
