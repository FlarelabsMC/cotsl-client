import QtQuick
import QtQuick.Effects

Item {
    id: btn
    signal clicked

    property string label: "Button"
    property color fillColor: "#e3bf91"
    property color borderColor: "#3c3c3c"
    property color glowColor: "#ffffff"
    property int glowRadius: 10
    property bool active: true
    property string source: "textures/launch/ui/gear.png"

    property real _glow: 0.0
    Behavior on _glow {
        NumberAnimation {
            duration: 150
        }
    }

    Image {
        id: img
        anchors.fill: parent
        anchors.margins: btn.glowRadius
        width: btn.width
        height: btn.height
        source: Qt.resolvedUrl(btn.source)
        smooth: false
        fillMode: Image.PreserveAspectCrop
        opacity: mouseArea.pressed ? 0.7 : 1.0

        layer.enabled: true

        layer.effect: MultiEffect {
            blurEnabled: true
            blurMax: btn.glowRadius
            blur: btn._glow * 0.6
        }
    }

    MouseArea {
        id: mouseArea
        anchors.fill: parent
        hoverEnabled: true
        enabled: active
        cursorShape: active ? Qt.PointingHandCursor : Qt.ArrowCursor
        onEntered: btn._glow = 1.0
        onExited: btn._glow = 0.0
        onClicked: btn.clicked()
    }
}
