import QtQuick
import QtQuick.Effects

Window {
    id: root
    width: 860; height: 520
    minimumWidth: 640; minimumHeight: 400
    visible: true
    flags: Qt.FramelessWindowHint | Qt.Window
    color: "#0f0d0b"

    onVisibleChanged: {
        if (visible) {
            x = (Screen.width - width) / 2
            y = (Screen.height - height) / 2
        }
    }

    property real targetMX: 0.5
    property real targetMY: 0.5

    property real smoothMX: 0.7
    Behavior on smoothMX { SmoothedAnimation { velocity: -0.5; duration: 400 } }
    property real smoothMY: 0.7
    Behavior on smoothMY { SmoothedAnimation { velocity: -0.5; duration: 400 } }

    Window {
        id: authWindow
        width: 240; height: 240
        flags: Qt.FramelessWindowHint | Qt.Window | Qt.WindowStaysOnTopHint
        color: "#0f0d0b"
        visible: authState === "auth-needed" || authState === "auth-waiting" || authState === "auth-error"

        onClosing: (close) => { close.accepted = false }

        onVisibleChanged: {
            if (visible) {
                x = (Screen.width - width) / 2
                y = (Screen.height - height) / 2
            }
        }

        Rectangle {
            id: authTitleBar
            anchors { top: parent.top; left: parent.left; right: parent.right }
            height: 32
            color: "#cc0f0d0b"

            DragHandler {
                onActiveChanged: if (active) authWindow.startSystemMove()
            }

            BitmapText {
                anchors.centerIn: parent
                text: "Sign in"
                textColor: "#b4a58c"
                charScale: 2
            }

            Image {
                id: authCloseBtn
                anchors { right: parent.right; rightMargin: 14; verticalCenter: parent.verticalCenter }
                width: 12; height: 14
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
            anchors { top: authTitleBar.bottom; bottom: parent.bottom; left: parent.left; right: parent.right }

            GlowButton {
                anchors.centerIn: parent
                width: 220; height: 60
                label: "Sign in"
                visible: authState === "auth-needed"
                onClicked: bridge.startAuth()
            }

            GlowButton {
                anchors.centerIn: parent
                width: 220; height: 60
                fillColor: '#a5be89'
                label: "Open in browser"
                visible: authState === "auth-waiting"
                onClicked: Qt.openUrlExternally(root.authUrl)
            }
        }
    }

    MouseArea {
        anchors.fill: parent
        hoverEnabled: true
        onPositionChanged: (mouse) => {
            root.targetMX = mouse.x / root.width
            root.targetMY = mouse.y / root.height
        }
        propagateComposedEvents: true
        onPressed: (mouse) => mouse.accepted = false
    }

    Timer {
        interval: 16; running: true; repeat: true
        onTriggered: {
            root.smoothMX = root.targetMX
            root.smoothMY = root.targetMY
        }
    }

    property string authState: "checking"
    property string authUrl: ""
    property string authCode: ""
    property string authErrorMsg: ""

    Component.onCompleted: authState = bridge.needsAuth() ? "auth-needed" : "launch"

    Connections {
        target: bridge
        function onAuthCodeReady(url, code) {
            root.authUrl = url
            root.authCode = code
            root.authState = "auth-waiting"
        }
        function onAuthDone() { root.authState = "launch" }
        function onAuthError(msg)  { root.authErrorMsg = msg; root.authState = "auth-error" }
        function onStartLaunch() {
            root.fadeActive = true
            sceneCapture.live = false
            fadeAnim.start()
        }
    }

    Item {
        id: bgContainer
        anchors.fill: parent
        clip: true

        Image {
            id: bgImage
            anchors.centerIn: parent
            width: parent.width * 1.3
            height: parent.height * 1.3
            source: Qt.resolvedUrl("textures/launch/bg/launch_bg.png")
            fillMode: Image.Stretch
            smooth: true

            transform: [
                Rotation {
                    origin.x: bgImage.width / 2
                    origin.y: bgImage.height / 2
                    axis { x: 0; y: 1; z: 0 }
                    angle: (root.smoothMX - 0.5) * 24
                },
                Rotation {
                    origin.x: bgImage.width / 2
                    origin.y: bgImage.height / 2
                    axis { x: 1; y: 0; z: 0 }
                    angle: -(root.smoothMY - 0.5) * 32
                }
            ]
        }
    }

    property real fadeProgress: 0.0
    property bool fadeActive: false

    ShaderEffectSource {
        id: sceneCapture
        sourceItem: bgContainer
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
        from: 0.0; to: 1.0
        duration: 800
        easing.type: Easing.InQuad
        onFinished: {
            root.visible = false
            bridge.launch()
            Qt.quit()
        }
    }

    // title bar
    Rectangle {
        id: titleBar
        anchors { top: parent.top; left: parent.left; right: parent.right }
        height: 32
        color: "#cc0f0d0b"

        DragHandler {
            onActiveChanged: if (active) root.startSystemMove()
        }

        BitmapText {
            anchors.centerIn: parent
            text: "Crypt of the Second Lord"
            textColor: "#b4a58c"
            charScale: 2
        }

        Image {
            id: closeBtn
            anchors { right: parent.right; rightMargin: 14; verticalCenter: parent.verticalCenter }
            width: 12; height: 14
            source: Qt.resolvedUrl("textures/launch/ui/x.png")
            smooth: false

            property bool hovered: false
            layer.enabled: true
            layer.effect: MultiEffect {
                colorizationColor: closeBtn.hovered ? "#dc503c" : "#e3bf91"
                colorization: 1.0
            }

            MouseArea {
                anchors.fill: parent
                hoverEnabled: true
                onEntered: closeBtn.hovered = true
                onExited: closeBtn.hovered = false
                onClicked: Qt.quit()
            }
        }
    }

    // bottom area, holds launch button and version
    Rectangle {
        id: bottomBar
        anchors { bottom: parent.bottom; left: parent.left; right: parent.right }
        height: 64
        color: "#cc0f0d0b"

        BitmapText {
            anchors { right: parent.right; bottom: parent.bottom; rightMargin: 12; bottomMargin: 12 }
            text: "1.0.0"
            textColor: "#b4a58c"
            charScale: 2
        }

        GlowButton {
            anchors { bottom: parent.bottom; horizontalCenter: parent.horizontalCenter; bottomMargin: 28 }
            width: 220; height: 72
            label: "Launch"
            visible: authState === "launch"
            onClicked: {
                bridge.beginLaunch();
            }
        }
    }
}