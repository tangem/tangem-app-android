package com.tangem.features.feed.ui

import androidx.compose.ui.graphics.Color
import com.tangem.features.feed.ui.utils.FadeConstants.FIRST_STEP
import com.tangem.features.feed.ui.utils.FadeConstants.FIRST_STEP_FADE_LEVEL

internal const val FEED_TOP_FADE_SOLID_STOP = 0.6f

internal fun feedTopFadeColorStops(fadeColor: Color): Array<Pair<Float, Color>> = arrayOf(
    0f to fadeColor,
    FIRST_STEP to fadeColor.copy(FIRST_STEP_FADE_LEVEL),
    1f to Color.Transparent,
)