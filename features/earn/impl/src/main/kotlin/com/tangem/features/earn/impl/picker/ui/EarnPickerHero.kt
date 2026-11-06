package com.tangem.features.earn.impl.picker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import kotlin.math.roundToInt

/**
 * The hero behind the top of the page: a gradient until the video arrives (spec §13). The frame keeps the 402 × 722
 * proportion of the design, but only its top 495 count as the page's height: the title, the cards and the description
 * lie over its lower part, which fades into the page background.
 */
@Composable
internal fun EarnPickerHero(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val width = constraints.maxWidth
                val frameHeight = (width * HERO_FRAME_HEIGHT / HERO_FRAME_WIDTH).roundToInt()
                val pageHeight = (width * HERO_PAGE_HEIGHT / HERO_FRAME_WIDTH).roundToInt()
                val placeable = measurable.measure(Constraints.fixed(width, frameHeight))
                layout(width, pageHeight) { placeable.place(0, 0) }
            }
            .background(TangemTheme.colors3.bg.tertiary),
    ) {
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

/** The title block: the fixed screen title and its line, 24 dp from the edges as the whole page. */
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

/** Where the title block starts in the design frame: the height the hero takes in the page. */
private const val HERO_PAGE_HEIGHT = 495f
private const val HERO_TINT_HEIGHT = 264f
private const val HERO_TINT_SOLID_FROM = 0.5f