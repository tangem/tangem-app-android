@file:Suppress("MagicNumber", "UnnecessaryParentheses")
package com.tangem.core.ui.components.background

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import com.tangem.core.ui.shader.TangemShader
import com.tangem.core.ui.shader.runtime.buildEffect
import kotlin.math.round

@Composable
fun Modifier.shaderBackground(
    shader: TangemShader,
    speed: Float = 1f,
    fallback: () -> Brush = {
        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    },
): Modifier {
    val runtimeEffect = remember(shader) { buildEffect(shader) }
    var size: Size by remember { mutableStateOf(Size(-1f, -1f)) }
    val speedModifier = shader.speedModifier

    val time by if (runtimeEffect.isSupported) {
        var startMillis = remember(shader) { -1L }
        produceState(0f, speedModifier) {
            while (true) {
                withInfiniteAnimationFrameMillis { frameTimeMillis ->
                    if (startMillis < 0) startMillis = frameTimeMillis
                    value = ((frameTimeMillis - startMillis) / 16.6f) / 10f
                }
            }
        }
    } else {
        remember { mutableFloatStateOf(-1f) }
    }

    return this then Modifier.onGloballyPositioned {
        size = Size(it.size.width.toFloat(), it.size.height.toFloat())
    }.drawBehind {
        runtimeEffect.update(
            shader = shader,
            time = (time * speed * speedModifier).round(3),
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

private fun Float.round(decimals: Int): Float {
    var multiplier = 1.0f
    repeat(decimals) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}