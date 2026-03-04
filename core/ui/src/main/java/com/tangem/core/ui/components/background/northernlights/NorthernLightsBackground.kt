@file:Suppress("MagicNumber")

package com.tangem.core.ui.components.background.northernlights

import android.os.Build
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.components.background.shaderBackground
import com.tangem.core.ui.res.LocalPowerSavingState
import com.tangem.core.ui.shader.NorthernLightsMeshGradientShader

/**
 * Animated northern lights background.
 * Uses a RuntimeShader on Android 13+ and falls back to a simpler implementation on older versions and in power saving mode.
 */
@Composable
fun NorthernLightsBackground(
    containerColor: Color,
    modifier: Modifier = Modifier,
    forceSimpleVersion: Boolean = false,
) {
    val isPowerSavingMode by LocalPowerSavingState.current.isPowerSavingModeEnabled.collectAsState()
    if (!forceSimpleVersion && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isPowerSavingMode) {
        NorthernLightsBackgroundWithShader(containerColor, modifier)
    } else {
        MovingColorfulBlubsBackground(modifier)
    }
}

@Suppress("LongMethod")
@Composable
private fun NorthernLightsBackgroundWithShader(containerColor: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "FluidMeshGradientV2")

    // Each track cycles through 4 states (matching the screenshot frames):
    //   deep/dark → saturated+bright → light/pastel → vibrant/vivid → back
    // 16 s total per track, staggered so no two tracks peak simultaneously.

    // ── Color 1 – indigo → bright blue → lavender → hot violet ──────────────
    val color1 by transition.animateColor(
        initialValue = Color(0xFF2A1480),
        targetValue = Color(0xFF2A1480),
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 16_000
                Color(0xFF2A1480) at 0 using FastOutSlowInEasing
                Color(0xFF4477EE) at 4_000 using FastOutSlowInEasing
                Color(0xFFBBAAEE) at 8_000 using FastOutSlowInEasing
                Color(0xFF8833EE) at 12_000 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "color1",
    )

    // ── Color 2 – dark blue → cyan-blue → sky → teal ─────────────────────────
    val color2 by transition.animateColor(
        initialValue = Color(0xFF1444AA),
        targetValue = Color(0xFF1444AA),
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 16_000
                Color(0xFF1444AA) at 0 using FastOutSlowInEasing
                Color(0xFF22AADD) at 4_000 using FastOutSlowInEasing
                Color(0xFF99BBDD) at 8_000 using FastOutSlowInEasing
                Color(0xFF44DDCC) at 12_000 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(4_000),
        ),
        label = "color2",
    )

    // ── Color 3 – dark purple → medium purple → rose pink → magenta ──────────
    val color3 by transition.animateColor(
        initialValue = Color(0xFF4422BB),
        targetValue = Color(0xFF4422BB),
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 16_000
                Color(0xFF4422BB) at 0 using FastOutSlowInEasing
                Color(0xFF7733CC) at 4_000 using FastOutSlowInEasing
                Color(0xFFDD88BB) at 8_000 using FastOutSlowInEasing
                Color(0xFFEE44AA) at 12_000 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(8_000),
        ),
        label = "color3",
    )

    // ── Color 4 – dark violet → medium violet → light pink → hot pink ────────
    val color4 by transition.animateColor(
        initialValue = Color(0xFF331199),
        targetValue = Color(0xFF331199),
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 16_000
                Color(0xFF331199) at 0 using FastOutSlowInEasing
                Color(0xFF6644CC) at 4_000 using FastOutSlowInEasing
                Color(0xFFCC77DD) at 8_000 using FastOutSlowInEasing
                Color(0xFFFF66CC) at 12_000 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(2_000),
        ),
        label = "color4",
    )

    // Keep a stable shader instance so the RuntimeShader is never recreated.
    // Colors are pushed each recomposition via updateColors().
    val shader = remember {
        NorthernLightsMeshGradientShader(
            colors = arrayOf(
                Color(0xFF2A1480),
                Color(0xFF1444AA),
                Color(0xFF4422BB),
                Color(0xFF331199),
                containerColor,
            ),
            speed = 0.5f,
            scale = 4f,
        )
    }
    val colorsArray = remember { Array(5) { Color.Unspecified } }
    colorsArray[0] = color1
    colorsArray[1] = color2
    colorsArray[2] = color3
    colorsArray[3] = color4
    colorsArray[4] = containerColor
    shader.updateColors(colorsArray)

    Box(
        modifier = modifier
            .background(containerColor)
            .fillMaxSize()
            .shaderBackground(shader),
    )
}