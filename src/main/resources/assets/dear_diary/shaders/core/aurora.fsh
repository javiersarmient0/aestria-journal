#version 150

in vec2 texCoord0;
in vec4 vertexColor;

uniform float GameTime;

out vec4 fragColor;

const float TAU = 6.28318530718;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
        mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x),
        f.y
    );
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; i++) {
        value += noise(p) * amplitude;
        p = p * 2.03 + vec2(17.1, 9.2);
        amplitude *= 0.5;
    }
    return value;
}

void main() {
    vec2 uv = texCoord0;
    float t = GameTime;

    // Circular domain keeps the procedural texture continuous at the ring seam.
    float angle = uv.x * TAU;
    vec2 ring = vec2(cos(angle), sin(angle));

    // Smooth, slowly moving distortion at multiple scales.
    float broadWarp = fbm(ring * 3.0 + vec2(t * 0.035, uv.y * 2.0 - t * 0.018));
    float fineWarp = fbm(ring * 8.0 + vec2(-t * 0.07, uv.y * 3.5 + t * 0.025));
    float bend = (broadWarp - 0.45) * 0.16 + (fineWarp - 0.5) * 0.045;

    float veilNoise = fbm(ring * 7.0 + vec2(t * 0.025 + bend, uv.y * 3.0 - t * 0.02));
    float filamentWave = 0.5 + 0.5 * sin((angle * 46.0 + bend * 18.0 + t * 0.11) * 6.2831853);
    float filaments = pow(max(filamentWave, 0.0), 8.0);
    float wisps = smoothstep(0.34, 0.78, veilNoise);
    float intensity = 0.12 + wisps * 0.26 + filaments * (0.18 + wisps * 0.66);

    // The top and bottom fade smoothly, with irregular edges instead of hard rectangles.
    float edgeNoise = fbm(ring * 5.0 + vec2(t * 0.02, 0.7));
    float topEdge = 0.80 + (edgeNoise - 0.5) * 0.14;
    float bottomEdge = 0.10 + (fineWarp - 0.5) * 0.10;
    float verticalFade = smoothstep(bottomEdge, bottomEdge + 0.20, uv.y)
                       * (1.0 - smoothstep(topEdge - 0.14, topEdge + 0.04, uv.y));

    float alpha = clamp(intensity * verticalFade * 0.62 * vertexColor.a, 0.0, 0.72);

    vec3 cyan = vec3(0.08, 0.78, 0.72);
    vec3 green = vec3(0.16, 0.95, 0.48);
    vec3 pale = vec3(0.50, 0.98, 0.84);
    float colorMix = clamp(0.20 + veilNoise * 0.82 + filaments * 0.12, 0.0, 1.0);
    vec3 auroraColor = mix(cyan, green, colorMix);
    auroraColor = mix(auroraColor, pale, clamp(filaments * 0.40, 0.0, 0.36));

    // The core shader uses additive blending (SRC_ALPHA, ONE).
    fragColor = vec4(auroraColor, alpha);
}