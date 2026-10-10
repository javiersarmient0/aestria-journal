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
    // Reconstruct this pixel's view ray and rotate it into world space.
    vec2 ndc = texCoord0 * 2.0 - 1.0;
    vec4 viewPoint = InvProjMat * vec4(ndc, 1.0, 1.0);
    vec3 viewRay = normalize(viewPoint.xyz / max(abs(viewPoint.w), 0.0001));
    vec3 worldRay = normalize(mat3(InvModelViewMat) * viewRay);

    float elevation = asin(clamp(worldRay.y, -1.0, 1.0));
    float azimuth = atan(worldRay.z, worldRay.x);
    float t = GameTime;

    // Keep the effect above the horizon, but do not draw one continuous
    // latitude ring. Its lower edge, upper edge and horizontal coverage vary
    // independently, producing separated aurora curtains.
    float skyMask = smoothstep(0.025, 0.11, elevation);
    if (skyMask <= 0.001) {
        fragColor = vec4(0.0);
        return;
    }

    float horizontalField = fbm(vec2(azimuth * 2.7 + t * 0.018, 3.1));
    float patchField = fbm(vec2(azimuth * 4.4 + t * 0.012, elevation * 3.0 - t * 0.025));
    float detailField = fbm(vec2(azimuth * 9.0 - t * 0.028, elevation * 7.0 + t * 0.04));

    // Sparse horizontal sections break the old all-the-way-around ring.
    float patches = smoothstep(0.405, 0.59, horizontalField);
    patches *= smoothstep(0.36, 0.64, patchField);

    // Different heights per direction create irregular, torn curtain edges.
    float lowerEdge = 0.075 + horizontalField * 0.18;
    float upperEdge = 0.34 + fbm(vec2(azimuth * 2.1 + 4.0, 1.7)) * 0.40;
    float lowerMask = smoothstep(lowerEdge, lowerEdge + 0.09, elevation);
    float upperMask = 1.0 - smoothstep(upperEdge, upperEdge + 0.14, elevation);
    float heightMask = lowerMask * upperMask;

    // Warped vertical folds, rather than a sinusoid that traces a circle.
    float warpedAzimuth = azimuth * 62.0
        + (horizontalField - 0.5) * 18.0
        + (detailField - 0.5) * 7.0
        + sin(elevation * 15.0 + t * 0.16) * 2.0;
    float folds = 0.5 + 0.5 * sin(warpedAzimuth);
    float filaments = pow(max(folds, 0.0), 11.0);

    float veil = smoothstep(0.40, 0.68, patchField);
    float fineVeil = smoothstep(0.43, 0.72, detailField);
    float intensity = patches * heightMask * skyMask
                    * (veil * 0.34 + fineVeil * 0.14 + filaments * (0.16 + veil * 0.68));

    float alpha = clamp(intensity * 0.86 * vertexColor.a, 0.0, 0.66);

    vec3 deepCyan = vec3(0.018, 0.28, 0.48);
    vec3 auroraGreen = vec3(0.055, 0.88, 0.43);
    vec3 mint = vec3(0.50, 1.0, 0.78);
    vec3 color = mix(deepCyan, auroraGreen, clamp(0.20 + patchField * 1.25, 0.0, 1.0));
    color = mix(color, mint, clamp(filaments * 0.52, 0.0, 0.46));

    fragColor = vec4(color, alpha);
}
