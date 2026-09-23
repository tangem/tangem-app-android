@file:Suppress("MagicNumber")

package com.tangem.features.feed.components.v2.blocks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.components.v2.blocks.analytics.FeedBlocksAnalytics
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.ui.v2.FeedShortcutCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Shortcut into the For You screen. Always offered. */
internal class ForYouBlockComponent(
    context: AppComponentContext,
    private val blocksAnalytics: FeedBlocksAnalytics,
) : FeedBlockComponent(context = context, key = KEY) {

    override val size: FeedBlock.Size = FeedBlock.Size.Small

    override val isVisible: StateFlow<Boolean> = MutableStateFlow(true)

    @Composable
    override fun Content(modifier: Modifier) {
        FeedShortcutCard(
            title = stringResourceSafe(R.string.for_you_title),
            onClick = ::onClick,
            modifier = modifier,
            icon = { PortfolioDonutIcon() },
        )
    }

    private fun onClick() {
        blocksAnalytics.onForYouClicked()
        router.push(FeedRoute.ForYou)
    }

    companion object {
        const val KEY = "for_you"
    }
}

/**
 * Portfolio-allocation donut. The shares are the design's sample allocation, not the user's — the
 * block gets real balances when the For you plug-in lands.
 *
 * The slices sit next to each other with no gap, so their round caps lap over one another; the grey
 * track behind them shows through the remainder they don't cover. Painted last-to-first, which puts
 * the largest slice on top with its caps tucked over its neighbours.
 */
@Composable
private fun PortfolioDonutIcon(modifier: Modifier = Modifier) {
    val accent = TangemTheme.colors3.icon.accent
    val slices = listOf(
        accent.blue to 0.48f,
        accent.violet to 0.13f,
        accent.red to 0.14f,
        accent.green to 0.10f,
    )
    val trackColor = TangemTheme.colors3.border.tertiary

    Canvas(modifier = modifier.size(size = 24.dp)) {
        val strokeWidth = 5.dp.toPx()
        val inset = strokeWidth / 2
        val arcSize = Size(width = size.width - strokeWidth, height = size.height - strokeWidth)
        val arcOffset = Offset(x = inset, y = inset)
        val arcStyle = Stroke(width = strokeWidth, cap = StrokeCap.Round)

        drawArc(
            color = trackColor,
            startAngle = DONUT_START_ANGLE_DEG,
            sweepAngle = FULL_CIRCLE_DEG,
            useCenter = false,
            topLeft = arcOffset,
            size = arcSize,
            style = arcStyle,
        )

        val startAngles = slices.runningFold(DONUT_START_ANGLE_DEG) { angle, (_, weight) ->
            angle + weight * FULL_CIRCLE_DEG
        }

        slices.indices.reversed().forEach { index ->
            val (color, weight) = slices[index]
            drawArc(
                color = color,
                startAngle = startAngles[index],
                sweepAngle = weight * FULL_CIRCLE_DEG,
                useCenter = false,
                topLeft = arcOffset,
                size = arcSize,
                style = arcStyle,
            )
        }
    }
}

private const val FULL_CIRCLE_DEG = 360f

/** 12 o'clock, so the largest slice starts where the eye does. */
private const val DONUT_START_ANGLE_DEG = -90f