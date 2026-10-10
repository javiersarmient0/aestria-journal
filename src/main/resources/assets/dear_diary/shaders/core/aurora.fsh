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

    float t = GameTime;

    // Project onto a tangent plane around a fixed world-space sky direction.
    // This avoids the azimuth singularity at the zenith and its fisheye-like
    // stretching. The pattern is anchored to world orientation, not the reticle.
    vec3 skyForward = normalize(vec3(0.18, 0.28, 1.0));
    vec3 skyRight = normalize(cross(vec3(0.0, 1.0, 0.0), skyForward));
    vec3 skyUp = normalize(cross(skyForward, skyRight));
    float forward = dot(worldRay, skyForward);

    if (forward <= 0.18 || worldRay.y <= 0.015) {
        fragColor = vec4(0.0);
        return;
    }

    vec2 skyCoord = vec2(
        dot(worldRay, skyRight) / forward,
        dot(worldRay, skyUp) / forward
    );

    // Keep a broad sky region and fade the boundaries, instead of wrapping
    // the effect into a full ring around the horizon.
    float regionMask = (1.0 - smoothstep(1.25, 1.65, abs(skyCoord.x)))
                     * smoothstep(-0.02, 0.10, skyCoord.y)
                     * (1.0 - smoothstep(1.10, 1.45, skyCoord.y));

    if (regionMask <= 0.001) {
        fragColor = vec4(0.0);
        return;
    }

    // Animated domain warping creates tall, narrow folds that flow sideways.
    float warp = fbm(vec2(skyCoord.x * 2.6 - t * 0.10, skyCoord.y * 2.1 + t * 0.045));
    float broad = fbm(vec2(skyCoord.x * 3.2 + t * 0.16, skyCoord.y * 1.8 - t * 0.08));
    float detail = fbm(vec2(skyCoord.x * 8.5 - t * 0.24, skyCoord.y * 5.4 + t * 0.14));

    float xWarped = skyCoord.x * 24.0
                  + (warp - 0.5) * 7.5
                  + sin(skyCoord.y * 8.0 - t * 0.25) * 1.6;
    float folds = 0.5 + 0.5 * sin(xWarped);
    float filaments = pow(max(folds, 0.0), 10.0);

    // Uneven curtain tops and bottoms vary across the width.
    float lowerEdge = 0.12 + warp * 0.18;
    float upperEdge = 0.48 + fbm(vec2(skyCoord.x * 2.0 + 7.0, t * 0.015)) * 0.60;
    float lowerMask = smoothstep(lowerEdge, lowerEdge + 0.12, skyCoord.y);
    float upperMask = 1.0 - smoothstep(upperEdge, upperEdge + 0.18, skyCoord.y);
    float heightMask = lowerMask * upperMask;

    // Broad veils are subtle; narrow filaments carry the vertical structure.
    float veil = smoothstep(0.40, 0.66, broad);
    float fineVeil = smoothstep(0.42, 0.70, detail);
    float intensity = regionMask * heightMask
                    * (veil * 0.30 + fineVeil * 0.10
                    + filaments * (0.18 + veil * 0.78));

    float alpha = clamp(intensity * 1.12 * vertexColor.a, 0.0, 0.72);

    vec3 deepCyan = vec3(0.018, 0.30, 0.50);
    vec3 auroraGreen = vec3(0.055, 0.92, 0.47);
    vec3 mint = vec3(0.55, 1.0, 0.80);
    vec3 color = mix(deepCyan, auroraGreen, clamp(0.20 + broad * 1.25, 0.0, 1.0));
    color = mix(color, mint, clamp(filaments * 0.56, 0.0, 0.50));

    fragColor = vec4(color, alpha);
}
