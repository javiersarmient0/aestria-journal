#version 150

in vec2 texCoord0;
in vec4 vertexColor;

uniform float GameTime;
uniform mat4 InvProjMat;
uniform mat4 InvModelViewMat;
uniform vec3 CameraPos;

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
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 3; i++) {
        value += noise(p) * amplitude;
        p = p * 2.03 + vec2(17.1, 9.2);
        amplitude *= 0.5;
    }
    return value;
}

void main() {
    vec2 ndc = texCoord0 * 2.0 - 1.0;
    vec4 viewPoint = InvProjMat * vec4(ndc, 1.0, 1.0);
    vec3 viewRay = normalize(viewPoint.xyz / max(abs(viewPoint.w), 0.0001));
    vec3 rayDir = normalize(mat3(InvModelViewMat) * viewRay);

    // Aurora volume bounds in world Y. Rays are sampled through this slab,
    // not projected onto a screen-facing plane or wrapped around the horizon.
    const float layerBottom = 150.0;
    const float layerTop = 260.0;

    if (rayDir.y <= 0.015) {
        fragColor = vec4(0.0);
        return;
    }

    float enterDist = (layerBottom - CameraPos.y) / rayDir.y;
    float exitDist = (layerTop - CameraPos.y) / rayDir.y;
    if (exitDist <= 0.0) {
        fragColor = vec4(0.0);
        return;
    }
    enterDist = max(enterDist, 0.0);

    float t = GameTime;
    float accumulated = 0.0;
    vec3 accumulatedColor = vec3(0.0);

    // Integrate a small number of samples through the atmospheric layer.
    // World XZ coordinates anchor the noise so camera rotation cannot drag it.
    const int STEPS = 8;
    float stepLength = (exitDist - enterDist) / float(STEPS);
    if (stepLength <= 0.0) {
        fragColor = vec4(0.0);
        return;
    }

    for (int i = 0; i < STEPS; i++) {
        float distanceAlongRay = enterDist + (float(i) + 0.5) * stepLength;
        vec3 worldPos = CameraPos + rayDir * distanceAlongRay;

        // Two stable world axes form long curtains. A smaller cross-axis
        // frequency separates the curtains into wide, irregular ribbons.
        vec2 worldXZ = worldPos.xz;
        float along = dot(worldXZ, vec2(0.78, 0.63));
        float across = dot(worldXZ, vec2(-0.63, 0.78));

        float drift = t * 1.7;
        float broad = fbm(vec2(across * 0.0035 + drift * 0.035,
                               worldPos.y * 0.010 - t * 0.08));
        float detail = fbm(vec2(across * 0.012 - drift * 0.09,
                                worldPos.y * 0.027 + t * 0.13));

        float warp = fbm(vec2(across * 0.004 + t * 0.025,
                              along * 0.0018 - t * 0.018));
        float folds = 0.5 + 0.5 * sin(across * 0.095
                                    + (warp - 0.5) * 8.0
                                    + sin(worldPos.y * 0.035 - t * 0.55) * 1.8
                                    + t * 0.35);
        float filaments = pow(max(folds, 0.0), 9.0);

        // Fade at the vertical limits to prevent hard slab edges.
        float verticalFade = smoothstep(layerBottom, layerBottom + 22.0, worldPos.y)
                           * (1.0 - smoothstep(layerTop - 28.0, layerTop, worldPos.y));
        float curtainCoverage = smoothstep(0.37, 0.63, broad);
        float density = curtainCoverage
                      * (0.13 * smoothstep(0.40, 0.70, detail)
                      + filaments * (0.18 + 0.62 * smoothstep(0.42, 0.68, broad)))
                      * verticalFade;

        float sampleAlpha = clamp(density * stepLength * 0.012, 0.0, 0.16);
        vec3 green = vec3(0.055, 0.92, 0.47);
        vec3 cyan = vec3(0.035, 0.38, 0.72);
        vec3 mint = vec3(0.55, 1.0, 0.82);
        vec3 color = mix(cyan, green, clamp(0.25 + broad, 0.0, 1.0));
        color = mix(color, mint, filaments * 0.42);

        accumulatedColor += color * sampleAlpha * (1.0 - accumulated);
        accumulated += sampleAlpha * (1.0 - accumulated);
    }

    float alpha = clamp(accumulated * 1.65 * vertexColor.a, 0.0, 0.72);
    if (alpha < 0.004) {
        fragColor = vec4(0.0);
        return;
    }

    fragColor = vec4(accumulatedColor / max(accumulated, 0.0001), alpha);
}