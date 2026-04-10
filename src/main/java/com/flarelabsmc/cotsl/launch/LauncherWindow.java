package com.flarelabsmc.cotsl.launch;

import io.qt.QtInvokable;
import io.qt.core.*;
import io.qt.gui.*;
import io.qt.qml.*;
import io.qt.quick.*;
import io.qt.widgets.QApplication;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.java.StepMCProfile;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class LauncherWindow {
    public static void create(CountDownLatch latch) throws Exception {
        // there's almost no need to use Vulkan in the launcher but since Minecraft is using it now, I might as well
        // Vulkan isn't supported on macOS, just use Metal if so
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("mac")) QQuickWindow.setGraphicsApi(QSGRendererInterface.GraphicsApi.Metal);
        else QQuickWindow.setGraphicsApi(QSGRendererInterface.GraphicsApi.Vulkan);
        List<String> appArgs = new ArrayList<>(List.of("CotSL"));
        String platformPluginPath = System.getProperty("cotsl.qt.platformPluginPath");
        if (platformPluginPath != null) {
            appArgs.add("-platformpluginpath");
            appArgs.add(platformPluginPath);
            System.out.println("[CotSL] Using bundled platform plugin at: " + platformPluginPath);
        }
        QApplication.initialize(appArgs.toArray(new String[0]));
        QApplication.setApplicationName("Crypt of the Second Lord");
        Path qmlRoot = extractResources();
        QQmlApplicationEngine engine = new QQmlApplicationEngine();
        String qmlImportPath = System.getProperty("cotsl.qt.qmlImportPath");
        if (qmlImportPath != null) engine.addImportPath(qmlImportPath.replace('\\', '/'));
        // this one is only really used if this is a dev environment or on a PC with the dev variables set if you're a GEEK
        String qtDir = System.getenv("QTDIR");
        if (qtDir != null) engine.addImportPath(qtDir.replace('\\', '/') + "/qml");
        LaunchBridge bridge = new LaunchBridge();
        engine.rootContext().setContextProperty("bridge", bridge);
        // actually print QML errors if anyone makes a mistake in the production code for some reason
        engine.warnings.connect(warnings -> warnings.forEach(w -> System.err.println("[QML ERROR] " + w)));
        engine.load(qmlRoot.resolve("Launcher.qml").toUri().toString());
        if (engine.rootObjects().isEmpty()) throw new IllegalStateException("[CotSL] QML failed to load");
        bridge.launchRequested.connect(latch::countDown);
        QApplication.exec();
    }

    /**
     * actually adds the QML resources to the launch process. yeah, this has to be done manually, sadly
     * @return the path of the final resources
     * @throws Exception
     */
    private static Path extractResources() throws Exception {
        Path tmp = Files.createTempDirectory("cotsl-qml-");
        tmp.toFile().deleteOnExit();
        String[] resources = {
                "/qml/Launcher.qml",
                "/qml/GlowButton.qml",
                "/qml/BitmapText.qml",
                "/qml/shaders/pixelate.frag.qsb",
                "/assets/cotsl/textures/launch/bg/launch_bg.png",
                "/assets/cotsl/textures/launch/ui/x.png",
                "/assets/cotsl/textures/font/ascii.png",
        };
        for (String res : resources) {
            URL url = LauncherWindow.class.getResource(res);
            if (url == null) continue;
            String rel = res.replaceFirst("^/assets/cotsl/", "")
                    .replaceFirst("^/qml/", "");
            Path dest = tmp.resolve(rel);
            Files.createDirectories(dest.getParent());
            try (InputStream in = url.openStream()) {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return tmp;
    }

    public static class LaunchBridge extends QObject {
        public final Signal0 launchRequested = new Signal0();
        public final Signal2<String, String> authCodeReady = new Signal2<>();
        public final Signal0 authDone = new Signal0();
        public final Signal1<String> authError = new Signal1<>();

        @QtInvokable
        public void launch() {
            launchRequested.emit();
        }

        @QtInvokable
        public boolean needsAuth() {
            try {
                InstallState state = InstallState.load(LaunchAgent.getInstallStateFile());
                if (state.authToken == null) return true;
                return System.currentTimeMillis() >= state.authExpiry;
            } catch (Exception e) {
                return true;
            }
        }

        @QtInvokable
        public void startAuth() {
            Thread.ofVirtual().start(() -> {
                try {
                    HttpClient httpClient = new HttpClient();
                    StepFullJavaSession.FullJavaSession session = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.getFromInput(
                            httpClient,
                            new StepMsaDeviceCode.MsaDeviceCodeCallback(code ->
                                    authCodeReady.emit(code.getDirectVerificationUri(), code.getUserCode())
                            )
                    );
                    StepMCProfile.MCProfile profile = session.getMcProfile();
                    File stateFile = LaunchAgent.getInstallStateFile();
                    InstallState state = InstallState.load(stateFile);
                    state.authToken = profile.getMcToken().getAccessToken();
                    state.playerName = profile.getName();
                    state.playerUuid = profile.getId().toString();
                    state.authExpiry = profile.getMcToken().getExpireTimeMs();
                    state.save(stateFile);
                    authDone.emit();
                } catch (Exception e) {
                    authError.emit(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                }
            });
        }
    }
}
