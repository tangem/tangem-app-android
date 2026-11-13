package com.tangem.core.ui.ds2.pagination

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme

internal const val PAGINATION_WINDOW_SIZE = 4

private const val ITEM_SIZE_DP = 8
private const val ITEM_SPACING_DP = 6
private const val ITEM_STEP_DP = ITEM_SIZE_DP + ITEM_SPACING_DP
private const val DOT_RADIUS_DP = 3f
private const val OVERFLOW_SCALE = 2f / 3f
private const val HIDDEN_SCALE = 0f

private const val TRANSITION_DURATION_MILLIS = 200
private val TransitionEasing = CubicBezierEasing(a = 0.5f, b = 0f, c = 0.5f, d = 1f)
private val FloatTransition: FiniteAnimationSpec<Float> = tween(TRANSITION_DURATION_MILLIS, easing = TransitionEasing)
private val ColorTransition: FiniteAnimationSpec<Color> = tween(TRANSITION_DURATION_MILLIS, easing = TransitionEasing)

/**
 * First page of the visible window after the active page moves to [currentPage].
 *
 * The window stays put while the active page is an inner one and shifts only when the active page
 * would land on an overflow item, so the active item is never an overflow one. A move by one page
 * shifts the window by one item; a jump re-centres it next to the active page.
 */
internal fun nextWindowStart(previousStart: Int, currentPage: Int, pageCount: Int): Int {
    if (pageCount <= PAGINATION_WINDOW_SIZE) return 0

    val maxStart = pageCount - PAGINATION_WINDOW_SIZE
    val start = previousStart.coerceIn(0, maxStart)
    val end = start + PAGINATION_WINDOW_SIZE - 1
    val page = currentPage.coerceIn(0, pageCount - 1)

    return when {
        page <= start && start > 0 -> (page - 1).coerceIn(0, maxStart)
        page >= end && end < pageCount - 1 -> (page - PAGINATION_WINDOW_SIZE + 2).coerceIn(0, maxStart)
        else -> start
    }
}

internal fun isOverflowItem(index: Int, windowStart: Int, pageCount: Int): Boolean {
    val windowEnd = windowStart + PAGINATION_WINDOW_SIZE - 1
    val isClippedStart = index == windowStart && windowStart > 0
    val isClippedEnd = index == windowEnd && windowEnd < pageCount - 1
    return isClippedStart || isClippedEnd
}

/**
 * Keeps the window between recompositions: where it lands depends on where it was, not only on
 * the active page. Updating it is idempotent for the same page, so a discarded composition is harmless.
 */
private class PaginationWindow(private val pageCount: Int) {

    private var start = 0

    fun moveTo(currentPage: Int): Int {
        start = nextWindowStart(start, currentPage, pageCount)
        return start
    }
}

@Composable
internal fun PaginationItems(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    val window = remember(pageCount) { PaginationWindow(pageCount) }
    val windowStart = window.moveTo(currentPage)
    val windowEnd = windowStart + PAGINATION_WINDOW_SIZE - 1
    val visibleSlots = pageCount.coerceIn(0, PAGINATION_WINDOW_SIZE)
    val width = (visibleSlots * ITEM_SIZE_DP + (visibleSlots - 1).coerceAtLeast(0) * ITEM_SPACING_DP).dp

    // One item past each edge stays composed at zero size, so it can grow in when the window shifts.
    val composedItems = (windowStart - 1).coerceAtLeast(0)..(windowEnd + 1).coerceAtMost(pageCount - 1)

    Box(modifier = modifier.size(width = width, height = ITEM_SIZE_DP.dp)) {
        for (index in composedItems) {
            key(index) {
                val isInWindow = index in windowStart..windowEnd
                PaginationItem(
                    slot = index - windowStart,
                    scale = when {
                        !isInWindow -> HIDDEN_SCALE
                        isOverflowItem(index, windowStart, pageCount) -> OVERFLOW_SCALE
                        else -> 1f
                    },
                    alpha = if (isInWindow) 1f else 0f,
                    isActive = index == currentPage,
                )
            }
        }
    }
}

@Composable
private fun PaginationItem(slot: Int, scale: Float, alpha: Float, isActive: Boolean) {
    val translation by animateFloatAsState(
        targetValue = (slot * ITEM_STEP_DP).toFloat(),
        animationSpec = FloatTransition,
        label = "paginationItemTranslation",
    )
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = FloatTransition,
        label = "paginationItemScale",
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = alpha,
        animationSpec = FloatTransition,
        label = "paginationItemAlpha",
    )
    val color by animateColorAsState(
        targetValue = if (isActive) TangemTheme.colors3.icon.primary else TangemTheme.colors3.icon.tertiary,
        animationSpec = ColorTransition,
        label = "paginationItemColor",
    )

    Box(
        modifier = Modifier
            .size(ITEM_SIZE_DP.dp)
            .graphicsLayer { translationX = translation.dp.toPx() }
            .drawBehind {
                drawCircle(
                    color = color,
                    radius = DOT_RADIUS_DP.dp.toPx() * animatedScale,
                    alpha = animatedAlpha,
                )
            },
    )
}