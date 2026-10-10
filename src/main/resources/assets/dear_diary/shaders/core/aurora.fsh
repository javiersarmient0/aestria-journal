#version 150

in vec2 texCoord0;
in vec4 vertexColor;

uniform float GameTime;

out vec4 fragColor;

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

    // Slowly bending curtain; several noise scales prevent a regular barcode pattern.
    float broadWarp = fbm(vec2(uv.x * 5.0 + t * 0.035, uv.y * 2.0 - t * 0.018));
    float fineWarp = fbm(vec2(uv.x * 17.0 - t * 0.07, uv.y * 3.5 + t * 0.025));
    float bend = (broadWarp - 0.45) * 0.16 + (fineWarp - 0.5) * 0.045;

    float x = uv.x + bend;
    float vertical = uv.y;

    // Thin filaments nested inside a broad, soft aurora veil.
    float veilNoise = fbm(vec2(x * 13.0 + t * 0.025, vertical * 3.0 - t * 0.02));
    float filamentWave = 0.5 + 0.5 * sin((x * 92.0 + bend * 36.0 + t * 0.11) * 6.2831853);
    float filaments = pow(max(filamentWave, 0.0), 7.0);
    float wisps = smoothstep(0.34, 0.78, veilNoise);
    float intensity = (0.16 + wisps * 0.30 + filaments * (0.20 + wisps * 0.65));

    // Soft fade at the top and bottom, with irregular upper and lower edges.
    float edgeNoise = fbm(vec2(x * 8.0 + t * 0.02, 0.7));
    float topEdge = 0.78 + (edgeNoise - 0.5) * 0.13;
    float bottomEdge = 0.10 + (fineWarp - 0.5) * 0.10;
    float verticalFade = smoothstep(bottomEdge, bottomEdge + 0.20, vertical)
                       * (1.0 - smoothstep(topEdge - 0.14, topEdge + 0.04, vertical));

    // Keep the glow luminous without filling the entire ribbon with opaque color.
    float alpha = clamp(intensity * verticalFade * 0.62 * vertexColor.a, 0.0, 0.72);

    vec3 cyan = vec3(0.08, 0.78, 0.72);
    vec3 green = vec3(0.16, 0.95, 0.48);
    vec3 pale = vec3(0.50, 0.98, 0.84);
    float colorMix = clamp(0.25 + veilNoise * 0.70 + filaments * 0.18, 0.0, 1.0);
    vec3 auroraColor = mix(cyan, green, colorMix);
    auroraColor = mix(auroraColor, pale, clamp(filaments * 0.45, 0.0, 0.38));

    // The JSON blend mode is additive (SRC_ALPHA, ONE).
    fragColor = vec4(auroraColor, alpha);
}