package com.flarelabsmc.cotsl.core.transform.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Impl from WayGL (MIT) > https://github.com/wired-tomato/WayGL/blob/reload/core/src/main/java/net/wiredtomato/waygl/core/Loader.java
@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Unique private static Boolean isWayland;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;initBackendSystem(Lcom/mojang/blaze3d/platform/BackendOptions;)Lnet/minecraft/util/TimeSource$NanoTimeSource;"))
    private void preGLFWInit(GameConfig gameConfig, CallbackInfo ci) {
        if (useWayland()) GLFW.glfwInitHint(GLFW.GLFW_PLATFORM, GLFW.GLFW_PLATFORM_WAYLAND);
    }

    @Unique
    private static Boolean useWayland() {
        if (isWayland == null) {
            String sessionType = System.getenv("XDG_SESSION_TYPE");
            if (sessionType == null) sessionType = "";
            isWayland = GLFW.glfwPlatformSupported(GLFW.GLFW_PLATFORM_WAYLAND) && sessionType.toLowerCase().startsWith("wayland");
        }
        return isWayland;
    }
}
