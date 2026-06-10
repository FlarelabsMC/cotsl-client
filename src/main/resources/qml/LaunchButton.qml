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
    property real bgOpacity: 1.0
    property color textColor: Qt.rgba(0.29, 0.28, 0.26, 1)

    property bool closed: false
    property bool hovered: false

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
    readonly property real uniformScale: height / sliceH
    readonly property int capWidth: leftW * uniformScale
    readonly property int midTileWidth: midW * uniformScale
    readonly property int capHeight: sliceH * uniformScale

    readonly property int fullOpenMidWidth: Math.max(0, width - (leftW + rightW) * uniformScale) - (hovered ? 0 : 14)
    property int midTargetWidth: closed ? 0 : fullOpenMidWidth

    height: sliceH * tileScale

    Item {
        id: bg
        width: (leftW + rightW) * uniformScale + midContainer.width
        height: parent.height
        anchors.horizontalCenter: parent.horizontalCenter
        opacity: bgOpacity

        Image {
            id: leftCap
            anchors.left: parent.left
            anchors.verticalCenter: parent.verticalCenter
            source: Qt.resolvedUrl("textures/launch/ui/button_3s.png")
            width: leftW * uniformScale
            height: sliceH * uniformScale
            sourceClipRect: Qt.rect(0, 0, leftW, sliceH)
            smooth: false
        }

        Item {
            id: midContainer
            anchors.left: leftCap.right
            anchors.verticalCenter: parent.verticalCenter

            width: btn.midTargetWidth
            height: parent.height
            clip: true

            Behavior on width {
                NumberAnimation {
                    duration: 350
                    easing.type: Easing.InOutQuad
                }
            }

            Repeater {
                model: Math.ceil(midContainer.width / midTileWidth)
                Image {
                    width: midTileWidth
                    height: sliceH * uniformScale
                    x: index * midTileWidth
                    y: (midContainer.height - sliceH * uniformScale) / 2
                    source: "textures/launch/ui/button_3s.png"
                    sourceClipRect: Qt.rect(leftW, 0, midW, sliceH)
                    smooth: false
                }
            }
        }

        Image {
            id: rightCap
            width: rightW * uniformScale
            height: sliceH * uniformScale
            anchors.left: midContainer.right
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
        anchors.centerIn: parent
        text: btn.label
        charScale: 2.0 * uniformScale
        textColor: textColor
    }

    MouseArea {
        anchors.fill: bg
        hoverEnabled: true
        enabled: active && !btn.closed
        cursorShape: active ? Qt.PointingHandCursor : Qt.ArrowCursor
        onEntered: {
            hovered = true;
        }
        onExited: {
            hovered = false;
        }
        onClicked: btn.clicked()
    }
}
