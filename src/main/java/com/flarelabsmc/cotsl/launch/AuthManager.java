package com.flarelabsmc.cotsl.launch;

import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.java.StepMCProfile;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;

import static com.flarelabsmc.cotsl.launch.LaunchAgent.log;

public class AuthManager {
    public static void authIfNeeded() throws Exception {
        InstallState.Options state = InstallState.get();
        if (state.authToken != null && System.currentTimeMillis() < state.authExpiry) {
            log("[CotSL] Using existing auth for " + state.playerName);
            return;
        }
        log("[CotSL] Starting Microsoft sign-in...");
        HttpClient httpClient = new HttpClient();
        StepFullJavaSession.FullJavaSession session = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.getFromInput(
                httpClient,
                new StepMsaDeviceCode.MsaDeviceCodeCallback(code -> {
                    log("[CotSL] Sign in at: " + code.getDirectVerificationUri());
                    log("[CotSL] Or visit https://www.microsoft.com/link and enter: " + code.getUserCode());
                })
        );
        StepMCProfile.MCProfile profile = session.getMcProfile();
        state.authToken = profile.getMcToken().getAccessToken();
        state.playerName = profile.getName();
        state.playerUuid = profile.getId().toString();
        state.authExpiry = profile.getMcToken().getExpireTimeMs();
        state.save();
        log("[CotSL] Signed in as: " + state.playerName);
    }
}
