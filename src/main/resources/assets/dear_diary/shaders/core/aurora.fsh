#version 150

in vec2 texCoord0;
in vec4 vertexColor;

uniform float GameTime;
uniform mat4 InvProjMat;
uniform mat4 InvModelViewMat;

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
    float sum = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 5; i++) {
        sum += noise(p) * amplitude;
        p = p * 2.02 + vec2(13.7, 9.2);
        amplitude *= 0.5;
    }
    return sum;
}

void main() {
    vec2 ndc = texCoord0 * 2.0 - 1.0;
    vec4 viewPoint = InvProjMat * vec4(ndc, 1.0, 1.0);
    vec3 viewRay = normalize(viewPoint.xyz / max(abs(viewPoint.w), 0.0001));
    vec3 worldRay = normalize(mat3(InvModelViewMat) * viewRay);

    float elevation = asin(clamp(worldRay.y, -1.0, 1.0));
    float azimuth = atan(worldRay.z, worldRay.x);
    float t = GameTime;

    // Restrict the effect to the upper sky, with a soft lower edge.
    float skyMask = smoothstep(0.015, 0.095, elevation);
    if (skyMask <= 0.001) {
        fragColor = vec4(0.0);
        return;
    }

    // Time offsets are deliberately stronger so the curtains visibly flow.
    vec2 flowA = vec2(azimuth * 2.8 + t * 0.22, elevation * 2.4 - t * 0.12);
    vec2 flowB = vec2(azimuth * 5.1 - t * 0.31, elevation * 5.8 + t * 0.20);
    vec2 flowC = vec2(azimuth * 11.0 + t * 0.42, elevation * 9.0 - t * 0.28);

    float broad = fbm(flowA);
    float patchField = fbm(flowB);
    float detailField = fbm(flowC);

    // Large separated curtains, but with enough coverage to remain visible.
    float patches = smoothstep(0.34, 0.53, broad);
    patches *= smoothstep(0.31, 0.54, patchField);

    // The bottom and top edges vary with azimuth, avoiding a uniform ring.
    float edgeNoise = fbm(vec2(azimuth * 2.2 - t * 0.09, 2.3));
    float lowerEdge = 0.045 + edgeNoise * 0.13;
    float upperEdge = 0.40 + fbm(vec2(azimuth * 2.0 + t * 0.07, 5.4)) * 0.48;
    float lowerMask = smoothstep(lowerEdge, lowerEdge + 0.11, elevation);
    float upperMask = 1.0 - smoothstep(upperEdge, upperEdge + 0.16, elevation);
    float heightMask = lowerMask * upperMask;

    // Vertical luminous folds, distorted by animated noise.
    float warpedAzimuth = azimuth * 58.0
        + (broad - 0.5) * 20.0
        + (detailField - 0.5) * 10.0
        + sin(elevation * 14.0 - t * 0.42 + broad * 5.0) * 2.8;
    float folds = 0.5 + 0.5 * sin(warpedAzimuth);
    float filaments = pow(max(folds, 0.0), 9.0);

    float veil = smoothstep(0.36, 0.64, patchField);
    float fineVeil = smoothstep(0.39, 0.68, detailField);
    float intensity = patches * heightMask * skyMask
                    * (veil * 0.52 + fineVeil * 0.22
                    + filaments * (0.22 + veil * 0.78));

    float alpha = clamp(intensity * 1.15 * vertexColor.a, 0.0, 0.78);

    vec3 deepCyan = vec3(0.018, 0.30, 0.50);
    vec3 auroraGreen = vec3(0.055, 0.92, 0.47);
    vec3 mint = vec3(0.55, 1.0, 0.80);
    vec3 color = mix(deepCyan, auroraGreen, clamp(0.18 + patchField * 1.35, 0.0, 1.0));
    color = mix(color, mint, clamp(filaments * 0.58, 0.0, 0.52));

    fragColor = vec4(color, alpha);
}
