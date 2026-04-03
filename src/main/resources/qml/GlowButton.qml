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

    property real _glow: 0.0
    Behavior on _glow { NumberAnimation { duration: 150 } }

    Rectangle {
        id: body
        anchors.fill: parent
        anchors.margins: btn.glowRadius
        radius: 0
        color: btn.fillColor
        opacity: mouseArea.pressed ? 0.7 : 1.0

        border.color: Qt.tint(btn.borderColor, Qt.rgba(btn.glowColor.r, btn.glowColor.g, btn.glowColor.b, btn._glow * 0.6))
        border.width: 1.5

        BitmapText {
            anchors.centerIn: parent
            text: btn.label
            charScale: 2.0
            textColor: Qt.rgba(0.29, 0.28, 0.26, 1)
        }

        layer.enabled: true
        layer.effect: MultiEffect {
            blurEnabled: true
            blurMax: btn.glowRadius
            blur: btn._glow * 0.6
            colorizationColor: btn.glowColor
            colorization: 0.0
        }
    }

    MouseArea {
        id: mouseArea
        anchors.fill: parent
        hoverEnabled: true
        onEntered: btn._glow = 1.0
        onExited: btn._glow = 0.0
        onClicked: btn.clicked()
    }
}