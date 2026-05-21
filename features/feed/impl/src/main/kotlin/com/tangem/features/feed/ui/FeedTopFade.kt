package com.tangem.features.feed.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.features.feed.ui.utils.FadeConstants.FIRST_STEP
import com.tangem.features.feed.ui.utils.FadeConstants.FIRST_STEP_FADE_LEVEL

/**
 * When `true`, top fade areas render as a solid color (no gradient) — used while the wallet
 * bottom sheet is collapsed so the peek header matches [LocalMainBottomSheetColor].
 */
internal val LocalBottomSheetTopFadeSolid = compositionLocalOf { false }

private const val EXPANDED_TOP_FADE_SOLID_STOP = 0.6f
private const val COLLAPSED_TOP_FADE_SOLID_STOP = 1f

@Composable
internal fun feedTopFadeSolidStop(): Float {
    return if (LocalBottomSheetTopFadeSolid.current) {
        COLLAPSED_TOP_FADE_SOLID_STOP
    } else {
        EXPANDED_TOP_FADE_SOLID_STOP
    }
}

@Composable
internal fun feedTopFadeColor(defaultFadeColor: Color): Color {
    return if (LocalBottomSheetTopFadeSolid.current) {
        LocalMainBottomSheetColor.current.value
    } else {
        defaultFadeColor
    }
}

@Composable
internal fun feedTopFadeColorStops(defaultFadeColor: Color): Array<Pair<Float, Color>> {
    if (LocalBottomSheetTopFadeSolid.current) {
        val solidColor = LocalMainBottomSheetColor.current.value
        return arrayOf(0f to solidColor, 1f to solidColor)
    }
    return arrayOf(
        0f to defaultFadeColor,
        FIRST_STEP to defaultFadeColor.copy(FIRST_STEP_FADE_LEVEL),
        1f to Color.Transparent,
    )
}