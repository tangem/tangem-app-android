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

/**
 * `true` where the calculator sits on a glass card and is its own haze source: that source is transparent over the
 * card, so the menu material alone blurs next to nothing and lets the sharp content through.
 */
internal val LocalEarnCalculatorOnGlass = staticCompositionLocalOf { false }

/**
 * An opaque-ish blurred base under the design system menu material. The effect fills its whole rectangle, so it is
 * clipped to the corners of the menu panel.
 */
@Composable
internal fun Modifier.frostedMenuBase(): Modifier = clip(MenuPanelShape).hazeEffectTangem(
    style = HazeStyle(
        backgroundColor = TangemTheme.colors3.bg.primary.copy(alpha = BASE_ALPHA),
        // An empty list would fall back to the tints of LocalHazeStyle
        tints = listOf(HazeTint(Color.Transparent)),
        blurRadius = BLUR_RADIUS,
    ),
) {
    // Haze stops the effect at the haze source bounds by default when the base alpha is 0.9 or less; the menu may
    // reach past them, so the default must not depend on BASE_ALPHA
    clipToAreasBounds = false
}

/** The corners of the design system menu panel (`TangemMenuPanel`). */
private val MenuPanelShape = RoundedCornerShape(32.dp)

/**
 * The opacity of the base under the blurred backdrop: only about 5% of the sharp content under the panel shows through.
 */
private const val BASE_ALPHA = 0.95f

/** The radius of the design system menu material, so the two blur layers coincide. */
private val BLUR_RADIUS = 32.dp