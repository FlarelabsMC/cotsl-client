import QtQuick
import QtQuick.Effects
import QtQuick3D
import QtQuick3D.Helpers

Window {
    id: root
    width: 860
    height: 520
    minimumWidth: 640
    minimumHeight: 400
    visible: true
    flags: Qt.FramelessWindowHint | Qt.Window
    color: "#0f0d0b"
    property real uiScale: 1

    function gcd(w, h) {
        return (h == 0) ? w : gcd(h, w % h);
    }

    function aspect() {
        var w = Screen.width;
        var h = Screen.height;
        var r = gcd(w, h);
        return {
            w: w / r,
            h: h / r
        };
    }

    Item {
        focus: true
        Keys.onPressed: event => {
            if (event.key == Qt.Key_F11) {
                root.visibility === Window.FullScreen ? root.visibility = Window.Windowed : root.showFullScreen();
            }
        }
    }

    onVisibleChanged: {
        if (visible) {
            x = (Screen.width - width) / 2;
            y = (Screen.height - height) / 2;
        }
    }

    property real targetMX: 0.5
    property real targetMY: 0.5

    property real smoothMX: 0.7
    Behavior on smoothMX {
        SmoothedAnimation {
            duration: 400
            maximumEasingTime: 200
            easing.type: Easing.OutQuad
        }
    }
    property real smoothMY: 0.7
    Behavior on smoothMY {
        SmoothedAnimation {
            duration: 400
            maximumEasingTime: 200
            easing.type: Easing.OutQuad
        }
    }

    property bool settingsAnim: false

    onVisibilityChanged: {
        settingsAnim = true;
        Qt.callLater(() => settingsAnim = false);
    }

    Window {
        id: authWindow
        width: 240
        height: 240
        flags: Qt.FramelessWindowHint | Qt.Window | Qt.WindowStaysOnTopHint
        color: "#0f0d0b"
        visible: authState === "auth-needed" || authState === "auth-waiting" || authState === "auth-error" || authState === "auth-checking"

        onClosing: close => {
            close.accepted = false;
        }

        onVisibleChanged: {
            if (visible) {
                x = (Screen.width - width) / 2;
                y = (Screen.height - height) / 2;
            }
        }

        Rectangle {
            id: authTitleBar
            anchors {
                top: parent.top
                left: parent.left
                right: parent.right
            }
            height: 32
            color: "#cc0f0d0b"

            DragHandler {
                onActiveChanged: if (active)
                    authWindow.startSystemMove()
            }

            BitmapText {
                anchors.centerIn: parent
                text: "Sign in"
                textColor: "#b4a58c"
                charScale: 2
            }

            Image {
                id: authCloseBtn
                anchors {
                    right: parent.right
                    rightMargin: 14
                    verticalCenter: parent.verticalCenter
                }
                width: 12
                height: 14
                source: Qt.resolvedUrl("textures/launch/ui/x.png")
                smooth: false
                property bool hovered: false
                layer.enabled: true
                layer.effect: MultiEffect {
                    colorizationColor: authCloseBtn.hovered ? "#dc503c" : "#e3bf91"
                    colorization: 1.0
                }
                MouseArea {
                    anchors.fill: parent
                    hoverEnabled: true
                    onEntered: authCloseBtn.hovered = true
                    onExited: authCloseBtn.hovered = false
                    onClicked: Qt.quit()
                }
            }
        }

        Item {
            anchors {
                top: authTitleBar.bottom
                bottom: parent.bottom
                left: parent.left
                right: parent.right
            }

            BitmapText {
                anchors.centerIn: parent
                text: "Signing in..."
                textColor: "#b4a58c"
                visible: authState === "auth-checking"
                charScale: 2
            }

            GlowButton {
                anchors.centerIn: parent
                width: 220
                height: 60
                label: "Sign in"
                visible: authState === "auth-needed"
                onClicked: bridge.startNewLogin()
            }

            GlowButton {
                anchors.centerIn: parent
                width: 220
                height: 60
                fillColor: '#a5be89'
                label: "Open in browser"
                visible: authState === "auth-waiting"
                onClicked: Qt.openUrlExternally(root.authUrl)
            }
        }
    }

    Timer {
        interval: 16
        running: true
        repeat: true
        onTriggered: {
            root.smoothMX = root.targetMX;
            root.smoothMY = root.targetMY;
        }
    }

    property string authState: "auth-checking"
    property string authUrl: ""
    property string authCode: ""
    property string authErrorMsg: ""
    property string username: ""

    property string launchState: "ready"

    Component.onCompleted: bridge.startLogin()

    Connections {
        target: bridge
        function onAuthCodeReady(url, code) {
            root.authUrl = url;
            root.authCode = code;
            root.authState = "auth-waiting";
        }
        function onAuthDone() {
            root.authState = "launch";
        }
        function onAuthError(msg) {
            root.authErrorMsg = msg;
            root.authState = "auth-error";
        }
        function onStartLaunch() {
            launchState = "launching";
            root.fadeActive = true;
            sceneCapture.live = false;
            fadeAnim.start();
        }
        function onAuthNeedsLogin() {
            root.authState = "auth-needed";
        }
        function onSetUsername(name) {
            root.username = name;
        }
    }

    View3D {
        id: world
        anchors.fill: parent
        renderMode: View3D.Offscreen

        environment: ExtendedSceneEnvironment {
            backgroundMode: SceneEnvironment.Color
            clearColor: thisFog.color
            fog: Fog {
                id: thisFog
                color: "#000000"
                depthNear: 3.0
                depthFar: 50.0
                enabled: true
                depthEnabled: true
            }
            fxaaEnabled: true
            ditheringEnabled: true
            tonemapMode: SceneEnvironment.TonemapModeAces
            vignetteEnabled: true
        }

        PerspectiveCamera {
            id: cam
            readonly property real xrot: -(root.smoothMX - 0.5) * 10 - (((root.smoothMX - 0.5) * 10) / 2)

            position: Qt.vector3d(0, 5, 15)
            eulerRotation.y: xrot
            eulerRotation.z: xrot
            eulerRotation.x: 60 - (root.smoothMY - 0.5) * 6
            fieldOfView: 90
        }

        DirectionalLight {
            eulerRotation.x: -45
            eulerRotation.y: 30
            brightness: 1.0
        }

        Model {
            id: ground

            geometry: PlaneGeometry {
                width: 200
                height: 200
            }

            position: Qt.vector3d(0, -40, 0)

            materials: [
                DefaultMaterial {
                    diffuseMap: Texture {
                        source: Qt.resolvedUrl("textures/launch/bg/ground.png")
                        tilingModeHorizontal: Texture.Repeat
                        tilingModeVertical: Texture.Repeat
                        scaleU: 8
                        scaleV: 8
                        minFilter: Texture.Nearest
                        magFilter: Texture.Nearest
                    }
                }
            ]
        }
    }

    MouseArea {
        anchors.fill: parent
        hoverEnabled: true
        onPositionChanged: mouse => {
            root.targetMX = mouse.x / root.width;
            root.targetMY = mouse.y / root.height;
        }
        propagateComposedEvents: true
        onPressed: mouse => mouse.accepted = false
    }

    property real fadeProgress: 0.0
    property bool fadeActive: false

    ShaderEffectSource {
        id: sceneCapture
        sourceItem: world
        hideSource: false
        live: !root.fadeActive
    }

    ShaderEffect {
        id: fadeEffect
        anchors.fill: parent
        visible: root.fadeActive
        opacity: root.fadeActive ? 1.0 : 0.0

        property var source: sceneCapture
        property real progress: root.fadeProgress
        property real imgWidth: root.width
        property real imgHeight: root.height

        fragmentShader: Qt.resolvedUrl("shaders/pixelate.frag.qsb")
    }

    // fade out (activated after launch button pressed)
    NumberAnimation {
        id: fadeAnim
        target: root
        property: "fadeProgress"
        from: 0.0
        to: 1.0
        duration: 800
        easing.type: Easing.InQuad
        onFinished: {
            root.visible = false;
            bridge.launch();
            Qt.quit();
        }
    }

    // title bar
    Rectangle {
        id: titleBar
        anchors {
            top: parent.top
            left: parent.left
            right: parent.right
        }
        height: 32
        color: "#cc0f0d0b"

        DragHandler {
            onActiveChanged: if (active)
                root.startSystemMove()
        }

        Image {
            id: status
            anchors {
                left: parent.left
                leftMargin: 12
                verticalCenter: parent.verticalCenter
            }
            width: 12
            height: 14
            source: Qt.resolvedUrl("textures/launch/ui/online.png")
            smooth: false

            BitmapText {
                anchors {
                    left: parent.right
                    leftMargin: 2
                    verticalCenter: parent.verticalCenter
                }
                text: username
                textColor: "#b4a58c"
                charScale: 2
            }
        }

        BitmapText {
            anchors.centerIn: parent
            text: "Crypt of the Second Lord"
            textColor: "#b4a58c"
            charScale: 2
        }

        MouseArea {
            id: mouseArea
            anchors.fill: parent
            hoverEnabled: false
            enabled: parent.enabled
            onDoubleClicked: root.visibility === Window.FullScreen ? root.visibility = Window.Windowed : root.showFullScreen()
        }

        Image {
            id: closeBtn
            anchors {
                right: parent.right
                rightMargin: 14
                verticalCenter: parent.verticalCenter
            }
            width: 12
            height: 14
            source: Qt.resolvedUrl("textures/launch/ui/x.png")
            smooth: false

            property bool hovered: false
            layer.enabled: true
            layer.effect: MultiEffect {
                colorizationColor: closeBtn.hovered ? "#dc503c" : "#e3bf91"
                colorization: 1.0
            }

            MouseArea {
                anchors {
                    verticalCenter: parent.verticalCenter
                    horizontalCenter: parent.horizontalCenter
                }
                width: parent.width + 3
                height: parent.height + 3
                hoverEnabled: true
                onEntered: closeBtn.hovered = true
                onExited: closeBtn.hovered = false
                onClicked: Qt.quit()
            }
        }

        Image {
            id: fullscreenBtn
            anchors {
                right: closeBtn.left
                rightMargin: 8
                verticalCenter: parent.verticalCenter
            }
            width: 12
            height: 14
            source: root.visibility === Window.FullScreen ? Qt.resolvedUrl("textures/launch/ui/windowed.png") : Qt.resolvedUrl("textures/launch/ui/fullscreen.png")
            smooth: false

            property bool hovered: false
            layer.enabled: true
            layer.effect: MultiEffect {
                colorizationColor: fullscreenBtn.hovered ? "#ffffff" : "#e3bf91"
                colorization: 1.0
            }

            MouseArea {
                anchors {
                    verticalCenter: parent.verticalCenter
                    horizontalCenter: parent.horizontalCenter
                }
                width: parent.width + 3
                height: parent.height + 3
                hoverEnabled: true
                onEntered: fullscreenBtn.hovered = true
                onExited: fullscreenBtn.hovered = false
                onClicked: root.visibility === Window.FullScreen ? root.visibility = Window.Windowed : root.showFullScreen()
            }
        }

        Image {
            id: minimizeBtn
            anchors {
                right: fullscreenBtn.left
                rightMargin: 8
                verticalCenter: parent.verticalCenter
            }
            width: 12
            height: 14
            source: Qt.resolvedUrl("textures/launch/ui/minimize.png")
            smooth: false

            property bool hovered: false
            layer.enabled: true
            layer.effect: MultiEffect {
                colorizationColor: minimizeBtn.hovered ? "#ffffff" : "#e3bf91"
                colorization: 1.0
            }

            MouseArea {
                anchors {
                    verticalCenter: parent.verticalCenter
                    horizontalCenter: parent.horizontalCenter
                }
                width: parent.width + 3
                height: parent.height + 3
                hoverEnabled: true
                onEntered: minimizeBtn.hovered = true
                onExited: minimizeBtn.hovered = false
                onClicked: root.showMinimized()
            }
        }
    }

    property bool settingsOpen: false

    // bottom area, holds launch and settings buttons
    Rectangle {
        id: bottomBar
        anchors {
            left: parent.left
            right: parent.right
            bottom: parent.bottom
            bottomMargin: root.settingsOpen || root.fadeActive ? -height : 0
            Behavior on bottomMargin {
                NumberAnimation {
                    duration: 300
                    easing.type: Easing.InOutQuad
                }
            }
        }
        height: 96 * root.uiScale
        color: "#00000000"

        Item {
            id: barTexture
            anchors.fill: parent
            clip: true

            readonly property int sliceW: 128
            readonly property int sliceH: 32
            readonly property real tileScale: 3.0 * root.uiScale
            readonly property int tileW: sliceW * tileScale
            readonly property int tileH: sliceH * tileScale

            Repeater {
                model: Math.ceil(barTexture.width / barTexture.tileW)

                Image {
                    width: barTexture.tileW
                    height: barTexture.tileH
                    x: index * barTexture.tileW
                    y: (barTexture.height - barTexture.tileH) / 2
                    source: Qt.resolvedUrl("textures/launch/ui/bottom_bar.png")
                    sourceClipRect: Qt.rect(0, 0, barTexture.sliceW, barTexture.sliceH)
                    smooth: false
                }
            }
        }

        LaunchButton {
            anchors {
                verticalCenter: parent.verticalCenter
                horizontalCenter: parent.horizontalCenter
                bottomMargin: 28
            }
            width: 256 * root.uiScale * 0.75
            height: 96 * root.uiScale * 0.75
            label: launchState === "ready" ? "" : launchState === "installing" ? "Installing..." : launchState === "launching" ? "Launching..." : "Loading..."
            visible: authState === "launch"
            active: launchState === "ready"
            onClicked: {
                launchState = "installing";
                bridge.beginLaunch();
            }
            closed: launchState === "installing" || launchState === "launching"
            bgOpacity: closed ? 0.0 : 1.0
            textColor: closed ? "#ffffff" : Qt.rgba(0.29, 0.28, 0.26, 1)

            Behavior on bgOpacity {
                NumberAnimation {
                    duration: 350
                    easing.type: Easing.InOutQuad
                }
            }

            Image {
                id: launchTxt
                width: 72 * 3 * root.uiScale
                height: parent.height
                anchors.horizontalCenter: parent.horizontalCenter
                anchors.verticalCenter: parent.verticalCenter
                source: Qt.resolvedUrl("textures/launch/ui/launch.png")
                smooth: false
                opacity: launchState === "ready" ? 1.0 : 0.0
            }
        }

        SettingsButton {
            id: settingsBtn
            anchors {
                right: parent.right
                rightMargin: 12
                verticalCenter: parent.verticalCenter
            }
            width: 72 * root.uiScale
            height: 72 * root.uiScale
            onClicked: settingsOpen = true
        }
    }

    Rectangle {
        id: settingsScreen
        anchors {
            top: titleBar.bottom
            bottom: parent.bottom
        }
        visible: root.settingsOpen || root.settingsAnim
        width: parent.width
        color: "#de0f0d0b"
        z: 999

        x: root.settingsOpen ? 0 : parent.width

        Behavior on x {
            enabled: !root.settingsAnim
            NumberAnimation {
                duration: 300
                easing.type: Easing.InOutQuad
            }
        }

        Image {
            id: settingsCloseBtn
            anchors {
                top: parent.top
                left: parent.left
                topMargin: 12
                leftMargin: 12
            }
            width: 12 * root.uiScale
            height: 14 * root.uiScale
            source: Qt.resolvedUrl("textures/launch/ui/x.png")
            smooth: false

            property bool hovered: false
            layer.enabled: true
            layer.effect: MultiEffect {
                colorizationColor: settingsCloseBtn.hovered ? "#dc503c" : "#e3bf91"
                colorization: 1.0
            }

            MouseArea {
                anchors {
                    verticalCenter: parent.verticalCenter
                    horizontalCenter: parent.horizontalCenter
                }
                width: parent.width + 3
                height: parent.height + 3
                hoverEnabled: true
                onEntered: settingsCloseBtn.hovered = true
                onExited: settingsCloseBtn.hovered = false
                onClicked: root.settingsOpen = false
            }
        }

        BitmapText {
            id: settingsTxt
            anchors {
                left: settingsCloseBtn.right
                leftMargin: 14
                verticalCenter: settingsCloseBtn.verticalCenter
            }
            text: "Settings"
            textColor: "#b4a58c"
            charScale: 2 * root.uiScale
        }

        Column {
            anchors {
                top: settingsCloseBtn.bottom
                topMargin: 12
            }
            anchors.fill: parent
            anchors.margins: 24
        }
    }
}
