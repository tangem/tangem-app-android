@file:Suppress("MagicNumber")
package com.tangem.core.ui.shader

import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.shader.runtime.RuntimeEffect

/**
 * A shader that creates a colorful, flowing "northern lights" effect.
 * @param colors The colors to display. The last provided color acts like a "background"
 * @param speed Adjust the speed of the movement
 * @param scale Adjusts the scale of the board. Higher number -> larger billboard -> smaller color blobs
 *
[REDACTED_AUTHOR]
 */
class NorthernLightsMeshGradientShader(
    colors: Array<Color>,
    speed: Float = 1f,
    scale: Float = 2f,
) : TangemShader {

    private val colorCount = colors.size
    private val colorUniforms = colors.flatMap {
        listOf(it.red, it.green, it.blue)
    }.toTypedArray().toFloatArray()
    private val ambientUniform = FloatArray(3)

    init {
        recomputeAmbient()
    }

    override val sksl = """
uniform float uTime;
uniform vec3 uResolution;
uniform vec3 uAmbient;

const int MAX_COLORS = $colorCount;
uniform vec3 uColor[MAX_COLORS];

//	Simplex 3D Noise 
//	by Ian McEwan, Ashima Arts
//  https://gist.github.com/patriciogonzalezvivo/670c22f3966e662d2f83
//
vec4 permute(vec4 x) {
    return mod(((x * 34.0) + 1.0) * x, 289.0);
}
vec4 taylorInvSqrt(vec4 r) {
    return 1.79284291400159 - 0.85373472095314 * r;
}

float snoise(vec3 v) {
    const vec2 C = vec2(1.0 / 6.0, 1.0 / 3.0);
    const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);

    // First corner
    vec3 i = floor(v + dot(v, C.yyy));
    vec3 x0 = v - i + dot(i, C.xxx);

    // Other corners
    vec3 g = step(x0.yzx, x0.xyz);
    vec3 l = 1.0 - g;
    vec3 i1 = min(g.xyz, l.zxy);
    vec3 i2 = max(g.xyz, l.zxy);

    //  x0 = x0 - 0. + 0.0 * C 
    vec3 x1 = x0 - i1 + 1.0 * C.xxx;
    vec3 x2 = x0 - i2 + 2.0 * C.xxx;
    vec3 x3 = x0 - 1. + 3.0 * C.xxx;

    // Permutations
    i = mod(i, 289.0);
    vec4 p = permute(permute(permute(i.z + vec4(0.0, i1.z, i2.z, 1.0)) + i.y + vec4(0.0, i1.y, i2.y, 1.0)) + i.x + vec4(0.0, i1.x, i2.x, 1.0));

    // Gradients
    // ( N*N points uniformly over a square, mapped onto an octahedron.)
    float n_ = 1.0 / 7.0; // N=7
    vec3 ns = n_ * D.wyz - D.xzx;

    vec4 j = p - 49.0 * floor(p * ns.z * ns.z);  //  mod(p,N*N)

    vec4 x_ = floor(j * ns.z);
    vec4 y_ = floor(j - 7.0 * x_);    // mod(j,N)

    vec4 x = x_ * ns.x + ns.yyyy;
    vec4 y = y_ * ns.x + ns.yyyy;
    vec4 h = 1.0 - abs(x) - abs(y);

    vec4 b0 = vec4(x.xy, y.xy);
    vec4 b1 = vec4(x.zw, y.zw);

    vec4 s0 = floor(b0) * 2.0 + 1.0;
    vec4 s1 = floor(b1) * 2.0 + 1.0;
    vec4 sh = -step(h, vec4(0.0));

    vec4 a0 = b0.xzyw + s0.xzyw * sh.xxyy;
    vec4 a1 = b1.xzyw + s1.xzyw * sh.zzww;

    vec3 p0 = vec3(a0.xy, h.x);
    vec3 p1 = vec3(a0.zw, h.y);
    vec3 p2 = vec3(a1.xy, h.z);
    vec3 p3 = vec3(a1.zw, h.w);

    //Normalise gradients
    vec4 norm = taylorInvSqrt(vec4(dot(p0, p0), dot(p1, p1), dot(p2, p2), dot(p3, p3)));
    p0 *= norm.x;
    p1 *= norm.y;
    p2 *= norm.z;
    p3 *= norm.w;

    // Mix final noise value
    vec4 m = max(0.6 - vec4(dot(x0, x0), dot(x1, x1), dot(x2, x2), dot(x3, x3)), 0.0);
    m = m * m;
    return 42.0 * dot(m * m, vec4(dot(p0, x0), dot(p1, x1), dot(p2, x2), dot(p3, x3)));
}

vec4 main( vec2 fragCoord ) {
    float mr = min(uResolution.x, uResolution.y);
    vec2 uv = (fragCoord * $scale - uResolution.xy) / mr;

    vec2 base = uv / 2;

    vec3 vColor = uColor[MAX_COLORS - 1];

    const vec2 frequency = vec2(0.7, 0.3);
    const float noiseFloor = 0.00001;
    float t = uTime * 0.005;

    for(int i = 0; i < MAX_COLORS - 1; i++) {
        float fi = float(i);
        float flow  = 5. + fi * 0.3;
        float speed = 6. * $speed + fi * 0.3;
        float seed  = 1. + fi * 4.;
        float noiseCeil = 0.6 + fi * 0.07;

        float noise = smoothstep(noiseFloor, noiseCeil, snoise(vec3(base.x * frequency.x, base.y * frequency.y - t * flow, t * speed + seed)));

        vColor = mix(vColor, uColor[i], noise);
    }

    vColor = max(vColor, uAmbient);

    // Elliptical falloff centred at the very top of the screen.
    // Using fragCoord directly (pixels) and uResolution for screen size.
    // Horizontal radius  ~ 80 % of screen width  → wide enough to cover corners.
    // Vertical   radius  ~ 45 % of screen height → controls how far down the glow reaches.
    vec2  topCenter = vec2(uResolution.x * 0.5, 0.0);
    vec2  delta     = fragCoord - topCenter;
    vec2  radii     = vec2(uResolution.x * 0.9, uResolution.y * 0.65);
    float normDist  = length(delta / radii);
    float alpha     = pow(1.0 - smoothstep(0.0, 1.0, normDist), 1.5);

    // Pre-multiplied alpha so the shader composites correctly over the dark background.
    return vec4(vColor * alpha, alpha);
}
    """

    /** Updates the animated colors in-place without recreating the shader. */
    fun updateColors(colors: Array<Color>) {
        colors.forEachIndexed { i, color ->
            colorUniforms[i * 3 + 0] = color.red
            colorUniforms[i * 3 + 1] = color.green
            colorUniforms[i * 3 + 2] = color.blue
        }
        recomputeAmbient()
    }

    private fun recomputeAmbient() {
        val count = colorCount - 1
        var r = 0f
        var g = 0f
        var b = 0f
        for (i in 0 until count) {
            r += colorUniforms[i * 3]
            g += colorUniforms[i * 3 + 1]
            b += colorUniforms[i * 3 + 2]
        }
        val scale = 0.5f / count
        ambientUniform[0] = r * scale
        ambientUniform[1] = g * scale
        ambientUniform[2] = b * scale
    }

    override fun applyUniforms(runtimeEffect: RuntimeEffect, time: Float, width: Float, height: Float) {
        super.applyUniforms(runtimeEffect = runtimeEffect, time = time, width = width, height = height)

        runtimeEffect.setFloatUniform(name = "uColor", values = colorUniforms)
        runtimeEffect.setFloatUniform(
            name = "uAmbient",
            value1 = ambientUniform[0],
            value2 = ambientUniform[1],
            value3 = ambientUniform[2],
        )
    }
}