package dev.amber.tv

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.max

/**
 * GPU-rendered ambient background. The shader updates independently of Compose,
 * keeping the clock and settings UI out of the per-frame recomposition path.
 */
internal class AuroraAmbientView(context: Context, private val visualStyle: Int) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var runtimeShader: RuntimeShader? = null
    private var animator: ValueAnimator? = null
    private val shaderSource = """
        uniform float2 resolution;
        uniform float time;
        uniform float style;
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / max(resolution, float2(1.0, 1.0));
            float t = time;
            float waveA = sin(uv.x * 5.4 + t * 0.13) * 0.105;
            float waveB = sin(uv.x * 10.2 - t * 0.085 + 1.4) * 0.045;
            float waveC = sin(uv.x * 2.1 + t * 0.055) * 0.065;
            float ridge = 0.47 + waveA + waveB + waveC;
            float veil1 = exp(-pow((uv.y - ridge) * 10.5, 2.0));
            float veil2 = exp(-pow((uv.y - (ridge + 0.115 + sin(uv.x * 7.0 - t * 0.07) * 0.035)) * 13.0, 2.0));
            float veil3 = exp(-pow((uv.y - (ridge - 0.105 + sin(uv.x * 4.0 + t * 0.06) * 0.04)) * 15.0, 2.0));
            float edge = smoothstep(0.0, 0.16, uv.x) * (1.0 - smoothstep(0.84, 1.0, uv.x));
            float glow = (veil1 * 0.54 + veil2 * 0.26 + veil3 * 0.20) * edge;
            float3 base = float3(0.008, 0.018, 0.035);
            float3 c1 = mix(float3(0.02, 0.72, 0.48), float3(0.10, 0.48, 0.98), step(0.5, style));
            float3 c2 = mix(float3(0.10, 0.40, 0.86), float3(0.62, 0.20, 0.92), step(1.5, style));
            float3 c3 = mix(float3(0.26, 0.84, 0.68), float3(0.98, 0.43, 0.22), step(2.5, style));
            float3 aurora = mix(c1, c2, smoothstep(0.15, 0.82, uv.x + sin(t * 0.04) * 0.18));
            aurora = mix(aurora, c3, 0.22 + 0.18 * sin(uv.x * 3.0 + t * 0.035));
            float verticalFade = smoothstep(0.08, 0.38, uv.y) * (1.0 - smoothstep(0.68, 0.98, uv.y));
            float3 color = base + aurora * glow * verticalFade * 0.95;
            float horizon = exp(-pow((uv.y - (ridge + 0.19)) * 4.2, 2.0)) * edge;
            color += aurora * horizon * 0.055;
            float stars = step(0.9972, fract(sin(dot(floor(fragCoord / 3.0), float2(127.1, 311.7))) * 43758.5453));
            color += stars * (0.18 + 0.12 * sin(t * 0.45 + uv.x * 19.0));
            float vignette = 1.0 - 0.34 * pow(length((uv - 0.5) * float2(1.0, 0.72)), 1.7);
            color *= vignette;
            return half4(half3(color), 1.0);
        }
    """.trimIndent()

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        if (Build.VERSION.SDK_INT >= 33) {
            runCatching {
                runtimeShader = RuntimeShader(shaderSource).also {
                    it.setFloatUniform("style", visualStyle.coerceIn(0, 3).toFloat())
                    paint.shader = it
                }
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (runtimeShader != null) {
            animator = ValueAnimator.ofFloat(0f, 10000f).apply {
                duration = 1_000_000L
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener {
                    runtimeShader?.setFloatUniform("time", (it.animatedValue as Float))
                    invalidate()
                }
                start()
            }
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val shader = runtimeShader
        if (shader != null) {
            shader.setFloatUniform("resolution", max(1, width).toFloat(), max(1, height).toFloat())
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        } else {
            val colors = when (visualStyle) {
                1 -> intArrayOf(0xFF061426.toInt(), 0xFF0B4770.toInt(), 0xFF070D1A.toInt())
                2 -> intArrayOf(0xFF10091F.toInt(), 0xFF44205F.toInt(), 0xFF080B19.toInt())
                3 -> intArrayOf(0xFF1A0C12.toInt(), 0xFF71351F.toInt(), 0xFF090D19.toInt())
                else -> intArrayOf(0xFF061A1C.toInt(), 0xFF0A594A.toInt(), 0xFF080D1B.toInt())
            }
            paint.shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), colors, null, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            paint.shader = null
        }
    }
}
