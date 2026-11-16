package com.tangem.features.earn.impl.picker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.tangem.core.media.VideoSurfaceState
import com.tangem.core.media.ui.VideoSurface
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import kotlin.math.roundToInt

/**
 * The flat `bg.tertiary` fill is the poster: it shows until the video's first frame is on screen, and stays without a
 * [video]. The bottom tint always lies over it.
 * The frame keeps the design's 402 × 722 proportion, but only its top 495 count as the page's height: the title, the
 * cards and the description lie over its lower part.
 */
@Composable
internal fun EarnPickerHero(video: VideoSurfaceState?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val width = constraints.maxWidth
                val frameHeight = (width * HERO_FRAME_HEIGHT / HERO_FRAME_WIDTH).roundToInt()
                val pageHeight = (width * HERO_PAGE_HEIGHT / HERO_FRAME_WIDTH).roundToInt()
                val placeable = measurable.measure(Constraints.fixed(width, frameHeight))
                layout(width, pageHeight) { placeable.place(0, 0) }
            },
    ) {
        if (video == null) {
            HeroPoster()
        } else {
            VideoSurface(state = video, modifier = Modifier.matchParentSize()) { HeroPoster() }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(HERO_TINT_HEIGHT / HERO_FRAME_HEIGHT)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        HERO_TINT_SOLID_FROM to TangemTheme.colors3.bg.primary,
                        1f to TangemTheme.colors3.bg.primary,
                    ),
                ),
        )
    }
}

@Composable
private fun BoxScope.HeroPoster() {
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(TangemTheme.colors3.bg.tertiary),
    )
}

@Composable
internal fun EarnPickerTitle(title: TextReference, subtitle: TextReference, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_MARGIN, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography3.heading.medium,
            color = TangemTheme.colors3.text.primary,
        )
        Text(
            text = subtitle.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

internal val PAGE_MARGIN = 24.dp

private const val HERO_FRAME_WIDTH = 402f
private const val HERO_FRAME_HEIGHT = 722f

private const val HERO_PAGE_HEIGHT = 495f
private const val HERO_TINT_HEIGHT = 264f
private const val HERO_TINT_SOLID_FROM = 0.5f