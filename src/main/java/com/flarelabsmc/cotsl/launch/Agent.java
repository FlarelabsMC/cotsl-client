package com.flarelabsmc.cotsl.launch;

import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.utils.tree.BasicClassProvider;

import java.io.File;
import java.lang.instrument.Instrumentation;

import static com.flarelabsmc.cotsl.launch.Launcher.logErrWith;

public class Agent {
    public static final String DIV = "Agent";

    public static void premain(String agentArgs, Instrumentation inst) throws Exception {
        Launcher.initLog();

        File selfJar = Launcher.findSelf();
        if (selfJar == null) {
            logErrWith("Couldn't find own JAR, something is severely wrong", DIV);
            return;
        }

        Launcher.loadExtraJars(inst, selfJar);

        TransformerManager transformerManager = new TransformerManager(new BasicClassProvider());
        transformerManager.addTransformer("com.flarelabsmc.cotsl.launch.transform.ModDiscovererTransformer");
        transformerManager.hookInstrumentation(inst);
    }
}
