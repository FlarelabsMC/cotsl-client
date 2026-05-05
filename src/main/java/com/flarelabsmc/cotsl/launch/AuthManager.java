package com.flarelabsmc.cotsl.launch;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.java.model.MinecraftProfile;
import net.raphimc.minecraftauth.java.model.MinecraftToken;
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;
import java.util.function.Consumer;

import static com.flarelabsmc.cotsl.launch.LaunchAgent.log;

public class AuthManager {

    public record AuthParameters(
        String playerName,
        UUID playerUuid,
        String minecraftToken
    ) {}

    public static AuthParameters getAuthParams() throws Exception {
        JsonObject jsonAccountState = getAuthStateJson();

        if (jsonAccountState == null) return null;

        HttpClient httpClient = createHttpClient();
        JavaAuthManager authManager = JavaAuthManager.fromJson(httpClient, jsonAccountState);

        authManager.getChangeListeners().add(() -> {
            try {
                Files.writeString(Paths.getAuthStatePath().toPath(), JavaAuthManager.toJson(authManager).toString());
            } catch (IOException e) {
                log("[CotSL-Auth] Failed to save auth state file: " + e);
            }
        });

        MinecraftProfile profile = authManager.getMinecraftProfile().getUpToDate();
        MinecraftToken token = authManager.getMinecraftToken().getUpToDate();
        log("[CotSL-Auth] Refreshed Minecraft auth tokens");

        return new AuthParameters(profile.getName(), profile.getId(), token.getToken());
    }

    public static boolean isSavedAuthStateValid() {
        JsonObject authState = getAuthStateJson();

        if (authState == null) return false;

        return authState.has("msaApplicationConfig")
                && authState.has("deviceType")
                && authState.has("deviceKeyPair")
                && authState.has("deviceId")
                && authState.has("msaToken");
    }

    private static JsonObject getAuthStateJson() {
        File accountState = Paths.getAuthStatePath();
        try {
            JsonReader reader = new JsonReader(new FileReader(accountState));
            return new Gson().fromJson(reader, JsonObject.class);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    public static void logIn(Consumer<MsaDeviceCode> deviceCodeConsumer) throws Exception {
        log("[CotSL-Auth] Beginning new log in process...");
        File authStatePath = Paths.getAuthStatePath();

        JavaAuthManager authManager = JavaAuthManager
                .create(createHttpClient())
                .login(DeviceCodeMsaAuthService::new, (Consumer<MsaDeviceCode>) code -> {
                    log("[CotSL-Auth] Auth code received...");
                    deviceCodeConsumer.accept(code);
                });

        Files.writeString(authStatePath.toPath(), JavaAuthManager.toJson(authManager).toString());

        authManager.getChangeListeners().add(() -> {
            try {
                Files.writeString(authStatePath.toPath(), JavaAuthManager.toJson(authManager).toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        authManager.getMinecraftToken().refreshIfExpired();
        MinecraftProfile profile = authManager.getMinecraftProfile().getUpToDate();

        log("[CotSL-Auth] New log in successful as " + profile.getName());
    }

    public static HttpClient createHttpClient() {
        return MinecraftAuth.createHttpClient("CotSL-Auth");
    }
}
