#version 440

layout(location = 0) in vec2 qt_TexCoord0;
layout(location = 0) out vec4 fragColor;

layout(binding = 1) uniform sampler2D source;

layout(std140, binding = 0) uniform buf {
    mat4 qt_Matrix;
    float qt_Opacity;
    float progress;
    float imgWidth;
    float imgHeight;
};

void main() {
    float blockSize = max(2.0, pow(progress, 0.6) * 64.0);

    vec2 pixCount = vec2(imgWidth, imgHeight) / blockSize;
    vec2 snappedUV = floor(qt_TexCoord0 * pixCount) / pixCount;

    vec4 color = texture(source, snappedUV);

    float fadeAlpha = clamp(progress * 2.0 - 1.0, 0.0, 1.0);
    fragColor = mix(color, vec4(0.0, 0.0, 0.0, 1.0), fadeAlpha) * qt_Opacity;
}