@file:Suppress("MagicNumber", "UnnecessaryParentheses")
package com.tangem.core.ui.components.background

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.tangem.core.ui.shader.TangemShader
import com.tangem.core.ui.shader.runtime.buildEffect
import kotlin.math.round

/** ~30 fps. The shaders animate slowly, so capping the redraw frequency is visually lossless. */
internal const val SHADER_FRAME_INTERVAL_MILLIS = 33L

@Composable
fun Modifier.shaderBackground(
    shader: TangemShader,
    speed: Float = 1f,
    fallback: () -> Brush = {
        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    },
): Modifier {
    val runtimeEffect = remember(shader) { buildEffect(shader) }
    val speedModifier = shader.speedModifier

    val timeState: State<Float> = if (runtimeEffect.isSupported) {
        produceState(0f, speedModifier) {
            var startMillis = -1L
            while (true) {
                withInfiniteAnimationFrameMillis { frameTimeMillis ->
                    if (startMillis < 0) startMillis = frameTimeMillis
                    // Quantized so the value changes at most once per SHADER_FRAME_INTERVAL_MILLIS:
                    // writing an equal value doesn't invalidate the draw, capping shader redraws below
                    // the panel refresh rate (which is a vsync every 8.3ms on 120Hz devices).
                    val elapsedMillis = (frameTimeMillis - startMillis) /
                        SHADER_FRAME_INTERVAL_MILLIS * SHADER_FRAME_INTERVAL_MILLIS
                    value = (elapsedMillis / 16.6f) / 10f
                }
            }
        }
    } else {
        remember { mutableFloatStateOf(-1f) }
    }

    // The draw lambda is remembered so recompositions (e.g. animated shader colors upstream) reuse
    // the same draw node: a fresh lambda would update the node and invalidate the draw on every
    // recomposition, redrawing at full refresh rate regardless of the quantized time above.
    val drawBlock: DrawScope.() -> Unit = remember(runtimeEffect, shader, speed, speedModifier, timeState, fallback) {
        {
            runtimeEffect.update(
                shader = shader,
                time = (timeState.value * speed * speedModifier).round(3),
                width = size.width,
                height = size.height,
            ) // set uniforms for the shaders

            if (runtimeEffect.isReady) {
                drawRect(brush = runtimeEffect.build())
            } else {
                drawRect(brush = fallback())
            }
        }
    }

    return this then Modifier.drawBehind(drawBlock)
}

private fun Float.round(decimals: Int): Float {
    var multiplier = 1.0f
    repeat(decimals) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}