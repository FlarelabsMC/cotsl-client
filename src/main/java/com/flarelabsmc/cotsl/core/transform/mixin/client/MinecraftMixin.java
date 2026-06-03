package com.flarelabsmc.cotsl.core.transform.mixin.client;

import com.flarelabsmc.cotsl.client.CotSLClient;
import com.flarelabsmc.cotsl.common.CotSL;
import com.mojang.blaze3d.platform.IconSet;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackResources;
import org.lwjgl.sdl.SDLPixels;
import org.lwjgl.sdl.SDLSurface;
import org.lwjgl.sdl.SDLVideo;
import org.lwjgl.sdl.SDL_Surface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.fifthlight.blazesdl.SDLError;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    @Final
    private Window window;

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/Window;setIcon(Lnet/minecraft/server/packs/PackResources;Lcom/mojang/blaze3d/platform/IconSet;)V"
            )
    )
    private void setIcon(Window instance, PackResources resources, IconSet iconSet) {
        NativeImage mainImage = null;
        SDL_Surface mainSurface = null;
        try {
            InputStream inputStream = CotSLClient.class.getResourceAsStream("/assets/cotsl/textures/launch/cotsl_icon.png");
            if (inputStream == null) {
                CotSL.LOGGER.info("Failed to get window icon");
                return;
            }
            mainImage = NativeImage.read(inputStream);
            mainSurface = createSDLSurface(mainImage);

            SDLVideo.SDL_SetWindowIcon(window.handle(), mainSurface);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (mainSurface != null) {
                SDLSurface.SDL_DestroySurface(mainSurface);
            }
            if (mainImage != null) {
                mainImage.close();
            }
        }
    }

    @Redirect(
            method = "updateTitle",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/Window;setTitle(Ljava/lang/String;)V"
            )
    )
    private void setTitle(Window instance, String title) {
        instance.setTitle("Crypt of the Second Lord");
    }

    private static SDL_Surface createSDLSurface(NativeImage image) {
        if (image.format() != NativeImage.Format.RGBA) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "createSDLSurface only works on RGBA images; have %s", image.format()));
        }
        var pixelsPtr = image.getPointer();
        if (pixelsPtr == 0) {
            throw new IllegalStateException("NativeImage pointer is null");
        }
        var surface = SDLSurface.nSDL_CreateSurfaceFrom(
                image.getWidth(),
                image.getHeight(),
                SDLPixels.SDL_PIXELFORMAT_RGBA32,
                pixelsPtr,
                image.getWidth() * 4
        );
        if (surface == 0L) {
            throw SDLError.handleError("SDL_CreateSurfaceFrom");
        }
        return SDL_Surface.createSafe(surface);
    }
}
