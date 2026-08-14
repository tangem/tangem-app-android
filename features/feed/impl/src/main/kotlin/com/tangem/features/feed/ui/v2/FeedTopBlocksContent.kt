package com.tangem.features.feed.ui.v2

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.glowring.TangemGlowRing
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.v2.FeedV2Component

private val BlockGap = 8.dp

// Derived from the api-declared row height so the semi-open detent matches the layout exactly.
private val LargeBlockHeight = FeedV2Component.TopBlocksHeight
private val SmallBlockHeight = (LargeBlockHeight - BlockGap) / 2
private val SmallBlockWidth = 120.dp
private val LargeBlockWidth = 200.dp
private val BlockCornerRadius = 20.dp
private val BlockShape = RoundedCornerShape(size = BlockCornerRadius)

/**
 * Placeholder top-blocks row: the For you / Add account shortcut column followed by large promo
 * cards. Layout matches the target design; real block content arrives with the block plug-ins.
 *
 * @param onForYouClick opens the For You screen on the feed stack
 */
@Composable
internal fun FeedTopBlocksContent(onForYouClick: () -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(space = BlockGap),
    ) {
        item(key = "shortcuts") {
            Column(verticalArrangement = Arrangement.spacedBy(space = BlockGap)) {
                ForYouBlock(onClick = onForYouClick)
                AddAccountBlock()
            }
        }
        item(key = "promo_pay") {
            LargeBlockPlaceholder(title = "Tangem Pay")
        }
        item(key = "promo_yield") {
            LargeBlockPlaceholder(title = "Yield")
        }
    }
}

/** For you shortcut: a small block with the DS3 magic [TangemGlowRing] running along its border. */
@Composable
private fun ForYouBlock(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(width = SmallBlockWidth, height = SmallBlockHeight)) {
        SmallBlock(
            title = "For you",
            onClick = onClick,
            modifier = Modifier.matchParentSize(),
            icon = { PortfolioDonutIcon() },
        )
        // the ring is an inner glow clipped to its bounds, so it goes OVER the card — inside the
        // ring's content slot the card's material fill would paint the whole glow over
        TangemGlowRing(
            modifier = Modifier.matchParentSize(),
            cornerRadius = BlockCornerRadius,
        )
    }
}

@Composable
private fun AddAccountBlock(modifier: Modifier = Modifier) {
    SmallBlock(
        title = "Add account",
        // TODO: [TWI-1608] add-account flow
        onClick = {},
        modifier = modifier.size(width = SmallBlockWidth, height = SmallBlockHeight),
        icon = { AddAccountIcon() },
    )
}

/** Small shortcut block: icon at the top start, title at the bottom start. */
@Composable
private fun SmallBlock(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    TangemSurface(
        modifier = modifier,
        isMaterial = true,
        isShadowEnabled = false,
        shape = BlockShape,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            icon()
            Text(
                text = title,
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.primary,
                maxLines = 1,
            )
        }
    }
}

/** Placeholder portfolio-allocation donut, segments in the DS3 accent colors. */
@Suppress("MagicNumber")
@Composable
private fun PortfolioDonutIcon(modifier: Modifier = Modifier) {
    val accent = TangemTheme.colors3.icon.accent
    val segments = listOf(
        accent.blue to 150f,
        accent.red to 40f,
        accent.green to 40f,
        accent.orange to 40f,
    )

    Canvas(modifier = modifier.size(size = 28.dp)) {
        val strokeWidth = 5.dp.toPx()
        val inset = strokeWidth / 2
        val arcSize = Size(width = size.width - strokeWidth, height = size.height - strokeWidth)
        val gap = 18f
        var startAngle = -90f

        segments.forEach { (color, sweep) ->
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(x = inset, y = inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            startAngle += sweep + gap
        }
    }
}

@Composable
private fun AddAccountIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size = 28.dp)
            .clip(shape = CircleShape)
            .background(color = TangemTheme.colors3.bg.accent.blue),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_plus_24),
            contentDescription = null,
            tint = TangemTheme.colors3.icon.staticLight,
            modifier = Modifier.size(size = 20.dp),
        )
    }
}

@Composable
private fun LargeBlockPlaceholder(title: String, modifier: Modifier = Modifier) {
    TangemSurface(
        modifier = modifier.size(width = LargeBlockWidth, height = LargeBlockHeight),
        isMaterial = true,
        isShadowEnabled = false,
        shape = BlockShape,
        onClick = {},
    ) {
        Box(modifier = Modifier.padding(all = 12.dp), contentAlignment = Alignment.Center) {
            Text(
                text = title,
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.primary,
            )
        }
    }
}