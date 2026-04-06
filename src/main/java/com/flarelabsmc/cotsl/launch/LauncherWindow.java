package com.flarelabsmc.cotsl.launch;

import io.qt.QtInvokable;
import io.qt.core.*;
import io.qt.gui.*;
import io.qt.qml.*;
import io.qt.quick.*;
import io.qt.widgets.QApplication;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class LauncherWindow {
    public static void create(CountDownLatch latch) throws Exception {
        QQuickWindow.setGraphicsApi(QSGRendererInterface.GraphicsApi.OpenGL);
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
        String qtDir = System.getenv("QTDIR");
        if (qtDir != null) engine.addImportPath(qtDir.replace('\\', '/') + "/qml");
        LaunchBridge bridge = new LaunchBridge();
        engine.rootContext().setContextProperty("bridge", bridge);
        engine.warnings.connect(warnings -> warnings.forEach(w -> System.err.println("[QML ERROR] " + w)));
        engine.load(qmlRoot.resolve("Launcher.qml").toUri().toString());
        if (engine.rootObjects().isEmpty()) throw new IllegalStateException("[CotSL] QML failed to load");
        bridge.launchRequested.connect(latch::countDown);
        QApplication.exec();
    }

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

        @QtInvokable
        public void launch() {
            launchRequested.emit();
        }
    }
}
