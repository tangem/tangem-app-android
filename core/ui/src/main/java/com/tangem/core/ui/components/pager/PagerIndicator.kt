package com.tangem.core.ui.components.pager

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlin.math.abs
import kotlin.math.min

// six - cause the central indicator has width multiplied twice
private const val TOTAL_MAX_INDICATORS = 6
private const val SPACER_COUNT_BETWEEN_INDICATORS = 4

/**
 * Horizontal pager indicator
 *
 * @param pagerState state of pager
 * @param indicatorCount counter of visible indicator items
 */
@Composable
fun PagerIndicator(pagerState: PagerState, modifier: Modifier = Modifier, indicatorCount: Int = 5) {
    if (pagerState.pageCount == 0) return

    val listState = rememberLazyListState()

    val indicatorColor = TangemTheme.colors.control.key
    val overlayColor = TangemTheme.colors.overlay.secondary

    val inactiveIndicatorColor = remember(indicatorColor) {
        indicatorColor.copy(alpha = 0.5f)
    }

    val baseIndicatorSize = 8.dp
    val spacing = 4.dp

    val indicatorState by remember(pagerState, indicatorCount) {
        derivedStateOf {
            val count = pagerState.pageCount
            val current = pagerState.currentPage

            val winSize = min(indicatorCount, count)
            val centerPosition = winSize / 2

            val start = when {
                count <= winSize -> 0
                current <= centerPosition -> 0
                current >= count - centerPosition - 1 -> count - winSize
                else -> current - centerPosition
            }
            Triple(count, winSize, start)
        }
    }

    val (itemCount, windowSize, windowStart) = indicatorState
    val currentItem by remember { derivedStateOf { pagerState.currentPage } }

    LaunchedEffect(currentItem, windowStart) {
        if (itemCount > windowSize) {
            listState.animateScrollToItem(windowStart.coerceIn(0, itemCount - 1))
        }
    }

    val maxContainerWidth = remember(baseIndicatorSize, spacing) {
        baseIndicatorSize * TOTAL_MAX_INDICATORS + spacing * SPACER_COUNT_BETWEEN_INDICATORS
    }

    Box(
        modifier = modifier
            .height(32.dp)
            .width(maxContainerWidth + 32.dp)
            .background(
                color = overlayColor,
                shape = CircleShape,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        LazyRow(
            modifier = Modifier.wrapContentWidth(),
            state = listState,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing),
            userScrollEnabled = false,
        ) {
            indicatorItems(
                itemCount = itemCount,
                currentItem = currentItem,
                activeColor = indicatorColor,
                inActiveColor = inactiveIndicatorColor,
                baseSize = baseIndicatorSize,
                windowSize = windowSize,
                windowStart = windowStart,
            )
        }
    }
}

@Suppress("MagicNumber", "CyclomaticComplexMethod")
private fun calculateIndicatorHeight(position: Int, currentPosition: Int, baseSize: Dp, windowSize: Int): Dp {
    val distance = abs(position - currentPosition)
    val mediumSize = 6.dp
    val smallSize = 4.dp

    if (windowSize < 5) {
        return when {
            distance <= 1 -> baseSize
            distance == 2 -> mediumSize
            else -> smallSize
        }
    }

    val isEdgeFocus = currentPosition == 0 || currentPosition == windowSize - 1
    val isNearEdgeFocus = currentPosition == 1 || currentPosition == windowSize - 2
    return when {
        isEdgeFocus -> when {
            distance <= 2 -> baseSize
            distance == 3 -> mediumSize
            else -> smallSize
        }
        isNearEdgeFocus -> when {
            distance <= 1 -> baseSize
            distance == 2 -> mediumSize
            else -> smallSize
        }
        else -> when {
            distance <= 1 -> baseSize
            else -> mediumSize
        }
    }
}

@Suppress("LongParameterList")
private fun LazyListScope.indicatorItems(
    itemCount: Int,
    currentItem: Int,
    activeColor: Color,
    inActiveColor: Color,
    baseSize: Dp,
    windowSize: Int,
    windowStart: Int,
) {
    val safeWindowSize = min(windowSize, itemCount)
    if (safeWindowSize <= 0) return

    val windowEnd = windowStart + safeWindowSize
    val currentPosInWindow = (currentItem - windowStart).coerceIn(0, safeWindowSize - 1)

    items(itemCount) { pageIndex ->
        val isInWindow = pageIndex in windowStart until windowEnd
        val positionInWindow = (pageIndex - windowStart).coerceIn(0, safeWindowSize - 1)

        val isSelected = pageIndex == currentItem

        val refinedHeight = if (isInWindow) {
            calculateIndicatorHeight(
                position = positionInWindow,
                currentPosition = currentPosInWindow,
                baseSize = baseSize,
                windowSize = safeWindowSize,
            )
        } else {
            0.dp
        }
        val targetWidth = if (isSelected) refinedHeight * 2 else refinedHeight
        val targetShape = if (isSelected) RoundedCornerShape(16.dp) else CircleShape
        val animatedWidth by animateDpAsState(targetValue = targetWidth, label = "width")
        val animatedHeight by animateDpAsState(targetValue = refinedHeight, label = "height")

        Box(
            modifier = Modifier
                .padding(vertical = (baseSize - animatedHeight) / 2)
                .clip(targetShape)
                .width(animatedWidth)
                .height(animatedHeight)
                .background(
                    if (isSelected) activeColor else inActiveColor,
                    targetShape,
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PagerIndicatorPreview() {
    TangemThemePreview {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors.background.primary)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val pagerState = rememberPagerState(
                initialPage = 2,
                pageCount = { 10 },
            )
            PagerIndicator(pagerState = pagerState)

            val pagerState1 = rememberPagerState(
                initialPage = 0,
                pageCount = { 3 },
            )
            PagerIndicator(pagerState = pagerState1)

            val pagerState2 = rememberPagerState(
                initialPage = 0,
                pageCount = { 1 },
            )
            PagerIndicator(pagerState = pagerState2)
        }
    }
}