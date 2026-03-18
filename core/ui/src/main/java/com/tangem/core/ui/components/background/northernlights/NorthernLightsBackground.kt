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
import com.tangem.core.ui.res.LocalIsInDarkTheme
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

@Suppress("LongMethod", "NamedArguments")
@Composable
private fun NorthernLightsBackgroundWithShader(containerColor: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "FluidMeshGradientV2")
    val isDark = LocalIsInDarkTheme.current

    @Composable
    fun anim(a: Color, b: Color, c: Color, d: Color, offset: Int = 0) = transition.animateColor(
        initialValue = a,
        targetValue = a,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 40_000
                a at 0 using FastOutSlowInEasing
                b at 10_000 using FastOutSlowInEasing
                c at 20_000 using FastOutSlowInEasing
                d at 30_000 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(offset),
        ),
        label = "c$offset",
    )

    val dc1 by anim(Color(0xFF0D0D3A), Color(0xFF141455), Color(0xFF1C1C6E), Color(0xFF111148), 0)
    val dc2 by anim(Color(0xFF0A1238), Color(0xFF0D1D55), Color(0xFF112266), Color(0xFF0E1A4A), 10_000)
    val dc3 by anim(Color(0xFF110A38), Color(0xFF1C1050), Color(0xFF2A1666), Color(0xFF180E48), 20_000)
    val dc4 by anim(Color(0xFF081A30), Color(0xFF0D2844), Color(0xFF113355), Color(0xFF0D2240), 5_000)

    val lc1 by anim(Color(0xFFCCB8EE), Color(0xFFBBA0E8), Color(0xFFCCB0F0), Color(0xFFC4AAEC), 0)
    val lc2 by anim(Color(0xFFB8C8F0), Color(0xFF9AAEE8), Color(0xFFAABEF0), Color(0xFFA0B8EE), 10_000)
    val lc3 by anim(Color(0xFFDDC8F5), Color(0xFFCCB0EE), Color(0xFFD8BEF5), Color(0xFFD0B8F2), 20_000)
    val lc4 by anim(Color(0xFFB8C4EE), Color(0xFFA8B4E8), Color(0xFFB4C0EE), Color(0xFFAABCEC), 5_000)

    val color1 = if (isDark) dc1 else lc1
    val color2 = if (isDark) dc2 else lc2
    val color3 = if (isDark) dc3 else lc3
    val color4 = if (isDark) dc4 else lc4

    // Keep a stable shader instance so the RuntimeShader is never recreated.
    // Colors are pushed each recomposition via updateColors().
    val shader = remember {
        NorthernLightsMeshGradientShader(
            colors = arrayOf(
                color1,
                color2,
                color3,
                color4,
                containerColor,
            ),
            speed = 0.07f,
            scale = 1.8f,
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