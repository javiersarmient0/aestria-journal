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
    // Reconstruct this pixel's view ray, then rotate it into world space.
    vec2 ndc = texCoord0 * 2.0 - 1.0;
    vec4 viewPoint = InvProjMat * vec4(ndc, 1.0, 1.0);
    vec3 viewRay = normalize(viewPoint.xyz / max(abs(viewPoint.w), 0.0001));
    vec3 worldRay = normalize(mat3(InvModelViewMat) * viewRay);

    // The aurora exists only in the upper sky. Because this is a world-space
    // direction, turning the camera reveals a different part of the same sky.
    float elevation = asin(clamp(worldRay.y, -1.0, 1.0));
    float skyMask = smoothstep(0.035, 0.12, elevation)
                  * (1.0 - smoothstep(0.66, 0.88, elevation));
    if (skyMask <= 0.001) {
        fragColor = vec4(0.0);
        return;
    }

    float azimuth = atan(worldRay.z, worldRay.x);
    float t = GameTime;

    // Two slowly drifting noise fields form wide curtains and finer folds.
    vec2 domain = vec2(azimuth * 4.2, elevation * 8.5);
    float broad = fbm(domain * vec2(1.1, 1.8) + vec2(t * 0.13, -t * 0.035));
    float detail = fbm(domain * vec2(3.7, 2.4) + vec2(-t * 0.22, t * 0.06));
    float curtainShape = broad * 0.72 + detail * 0.28;

    float folds = 0.5 + 0.5 * sin(
        azimuth * 58.0 + (broad - 0.5) * 13.0
        + sin(elevation * 17.0 + t * 0.17) * 2.2
    );
    float filaments = pow(max(folds, 0.0), 10.0);
    float veil = smoothstep(0.36, 0.72, curtainShape);
    float verticalRibbons = smoothstep(0.08, 0.20, elevation + (broad - 0.5) * 0.12)
                          * (1.0 - smoothstep(0.48, 0.67, elevation + (detail - 0.5) * 0.09));

    float intensity = (veil * 0.38 + filaments * (0.20 + veil * 0.8))
                    * verticalRibbons * skyMask;
    float alpha = clamp(intensity * 0.72 * vertexColor.a, 0.0, 0.68);

    vec3 deepCyan = vec3(0.025, 0.38, 0.54);
    vec3 auroraGreen = vec3(0.08, 0.92, 0.47);
    vec3 mint = vec3(0.48, 1.0, 0.78);
    vec3 color = mix(deepCyan, auroraGreen, clamp(0.25 + curtainShape * 1.1, 0.0, 1.0));
    color = mix(color, mint, clamp(filaments * 0.55, 0.0, 0.48));

    fragColor = vec4(color, alpha);
}