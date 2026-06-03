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

    property real _glow: 0.0
    Behavior on _glow {
        NumberAnimation {
            duration: 150
        }
    }

    readonly property int leftW: 8
    readonly property int midW: 32
    readonly property int rightW: 8
    readonly property int sliceH: 24
    readonly property real tileScale: 3.0
    readonly property int tileW: midW * tileScale
    readonly property int tileH: sliceH * tileScale
    readonly property real capScale: 3.0

    Item {
        id: bg
        anchors.fill: parent

        Image {
            id: leftCap
            width: leftW * capScale
            height: sliceH * tileScale
            anchors.left: parent.left
            anchors.verticalCenter: parent.verticalCenter
            source: Qt.resolvedUrl("textures/launch/ui/button_3s.png")
            sourceClipRect: Qt.rect(0, 0, leftW, sliceH)
            smooth: false
        }

        Item {
            id: mid
            anchors.left: leftCap.right
            anchors.right: rightCap.left
            anchors.top: parent.top
            anchors.bottom: parent.bottom
            clip: true

            Repeater {
                model: Math.ceil(mid.width / tileW)
                Image {
                    width: tileW
                    height: tileH
                    x: index * tileW
                    y: (mid.height - tileH) / 2
                    source: "textures/launch/ui/button_3s.png"
                    sourceClipRect: Qt.rect(leftW, 0, midW, sliceH)
                    smooth: false
                }
            }
        }

        Image {
            id: rightCap
            width: rightW * capScale
            height: sliceH * tileScale
            anchors.right: parent.right
            anchors.verticalCenter: parent.verticalCenter
            source: Qt.resolvedUrl("textures/launch/ui/button_3s.png")
            sourceClipRect: Qt.rect(leftW + midW, 0, rightW, sliceH)
            smooth: false
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

    BitmapText {
        anchors.verticalCenter: btn.verticalCenter
        anchors.horizontalCenter: btn.horizontalCenter
        text: btn.label
        charScale: 2.0
        textColor: Qt.rgba(0.29, 0.28, 0.26, 1)
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
