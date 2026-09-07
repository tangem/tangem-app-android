package com.tangem.features.polymarket.impl.onboarding.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.R as UiR

// Hero geometry as proportions of the design's 402pt-wide frame: the photo is 608 tall while the content
// column starts at 495, so the headline overlaps the photo's lower edge.
internal const val HERO_FRAME_WIDTH = 402f
internal const val HERO_IMAGE_HEIGHT = 608f
internal const val HERO_CONTENT_TOP = 495f
private const val TINT_START = 380f / HERO_IMAGE_HEIGHT
private const val TINT_END = 600f / HERO_IMAGE_HEIGHT

@Composable
internal fun PolymarketOnboardingHero(modifier: Modifier = Modifier) {
    val tintColor = TangemTheme.colors3.bg.primary

    Image(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(HERO_FRAME_WIDTH / HERO_IMAGE_HEIGHT)
            .drawWithCache {
                val tint = Brush.verticalGradient(
                    colorStops = arrayOf(
                        TINT_START to Color.Transparent,
                        TINT_END to tintColor,
                        1f to tintColor,
                    ),
                )
                onDrawWithContent {
                    drawContent()
                    drawRect(tint)
                }
            },
        painter = painterResource(UiR.drawable.img_prediction_welcome_hero),
        contentDescription = null,
        contentScale = ContentScale.Crop,
    )
}