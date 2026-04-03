import QtQuick
import QtQuick.Effects

Item {
    id: root
    property string text: ""
    property color textColor: "#ffffff"
    property real charScale: 2.0

    readonly property int cell: 8
    readonly property int advance: 6

    width: text.length > 0 ? (text.length - 1) * advance * charScale + cell * charScale : 0
    height: cell * charScale

    Row {
        spacing: (root.advance - root.cell) * root.charScale
        Repeater {
            model: root.text.length
            Image {
                required property int index
                width:  root.cell * root.charScale
                height: root.cell * root.charScale
                source: Qt.resolvedUrl("textures/font/ascii.png")
                sourceClipRect: Qt.rect(
                    (root.text.charCodeAt(index) % 16) * root.cell,
                    Math.floor(root.text.charCodeAt(index) / 16) * root.cell,
                    root.cell, root.cell
                )
                smooth: false
                fillMode: Image.Stretch
            }
        }
    }

    layer.enabled: true
    layer.effect: MultiEffect {
        colorizationColor: root.textColor
        colorization: 1.0
    }
}