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

/** `true` where the calculator is its own haze source over a glass card: the menu material alone shows it through. */
internal val LocalEarnCalculatorOnGlass = staticCompositionLocalOf { false }

@Composable
internal fun Modifier.frostedMenuBase(): Modifier = clip(RoundedCornerShape(32.dp)).hazeEffectTangem(
    style = HazeStyle(
        // 0.95 lets only about 5% of the sharp content under the panel show through
        backgroundColor = TangemTheme.colors3.bg.primary.copy(alpha = 0.95f),
        // An empty list would fall back to the tints of LocalHazeStyle
        tints = listOf(HazeTint(Color.Transparent)),
        blurRadius = 32.dp,
    ),
) {
    // Haze clips the effect to the source bounds by default only at a base alpha of 0.9 or less; the menu may reach
    // past them, so this must not depend on the alpha
    clipToAreasBounds = false
}