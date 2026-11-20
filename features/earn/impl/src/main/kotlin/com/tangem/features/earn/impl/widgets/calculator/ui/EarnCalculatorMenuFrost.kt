package com.tangem.features.earn.impl.widgets.calculator.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.res.TangemTheme
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint

internal val LocalEarnCalculatorOnGlass = staticCompositionLocalOf { false }

@Composable
internal fun Modifier.frostedMenuBase(): Modifier = clip(RoundedCornerShape(32.dp)).hazeEffectTangem(
    style = HazeStyle(
        backgroundColor = TangemTheme.colors3.bg.primary.copy(alpha = 0.95f),
        tints = listOf(HazeTint(Color.Transparent)),
        blurRadius = 32.dp,
    ),
) {
    clipToAreasBounds = false
}