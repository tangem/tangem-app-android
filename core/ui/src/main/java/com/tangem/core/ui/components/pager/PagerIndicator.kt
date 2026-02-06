package com.tangem.core.ui.components.pager

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

private const val ANIMATION_DURATION = 300
private const val MAX_VISIBLE_DOTS = 5
private const val MIN_HIDDEN_FOR_SMALL_DOT = 2
private const val MIN_DISTANCE_FOR_SMALL_DOT = 3
private const val MIN_DISTANCE_FOR_HINT_DOT = 2

private val SPACING = 4.dp
private val BACKGROUND_SIZE = DpSize(92.dp, 32.dp)

private val CURRENT_DOT_SIZE = DpSize(16.dp, 8.dp)
private val NORMAL_DOT_SIZE = DpSize(8.dp, 8.dp)
private val HINT_DOT_SIZE = DpSize(6.dp, 6.dp)
private val SMALL_DOT_SIZE = DpSize(4.dp, 4.dp)

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun PagerIndicator(pagerState: PagerState, modifier: Modifier = Modifier) {
    val totalPages = pagerState.pageCount
    val currentIndex = pagerState.currentPage

    if (totalPages == 0) return

    val indicatorColor = TangemTheme.colors.control.key
    val overlayColor = TangemTheme.colors.overlay.secondary
    val inactiveIndicatorColor = TangemTheme.colors.text.tertiary

    val density = LocalDensity.current

    val (targetLower, targetUpper) = getWindowBounds(totalPages, currentIndex)

    var displayLower by remember { mutableIntStateOf(targetLower) }
    var displayUpper by remember { mutableIntStateOf(targetUpper) }
    var prevTargetLower by remember { mutableIntStateOf(targetLower) }

    val slideOffset = remember { Animatable(0f) }
    var isSliding by remember { mutableStateOf(false) }
    var slideDirection by remember { mutableIntStateOf(0) }
    val fadeProgress = remember { Animatable(0f) }
    var fadeJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(targetLower) {
        if (targetLower != prevTargetLower && totalPages > MAX_VISIBLE_DOTS) {
            fadeJob?.cancel()
            slideOffset.stop()
            fadeProgress.stop()

            val dir = if (targetLower > prevTargetLower) 1 else -1
            val edgeDotSize = with(density) { (HINT_DOT_SIZE.width + SPACING).toPx() }
            val halfEdge = edgeDotSize / 2

            isSliding = true
            slideDirection = dir
            fadeProgress.snapTo(0f)

            if (dir > 0) {
                displayLower = prevTargetLower
                displayUpper = targetUpper
                slideOffset.snapTo(halfEdge)
            } else {
                displayLower = targetLower
                displayUpper = prevTargetLower + MAX_VISIBLE_DOTS
                slideOffset.snapTo(-halfEdge)
            }

            prevTargetLower = targetLower

            fadeJob = launch {
                fadeProgress.animateTo(1f, tween(ANIMATION_DURATION))
            }
            slideOffset.animateTo(
                if (dir > 0) -halfEdge else halfEdge,
                tween(ANIMATION_DURATION),
            )

            displayLower = targetLower
            displayUpper = targetUpper
            slideOffset.snapTo(0f)
            isSliding = false
            slideDirection = 0
        }
    }
    val visibleIndices = (displayLower until displayUpper).toList()

    Box(
        modifier = modifier
            .width(BACKGROUND_SIZE.width)
            .height(BACKGROUND_SIZE.height)
            .background(
                color = overlayColor,
                shape = CircleShape,
            )
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.offset {
                IntOffset(slideOffset.value.roundToInt(), 0)
            },
            horizontalArrangement = Arrangement.spacedBy(SPACING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            visibleIndices.forEach { index ->
                val dotAlpha = when {
                    !isSliding -> 1f
                    slideDirection > 0 && index == displayLower -> 1f - fadeProgress.value
                    slideDirection > 0 && index == displayUpper - 1 -> fadeProgress.value
                    slideDirection < 0 && index == displayUpper - 1 -> 1f - fadeProgress.value
                    slideDirection < 0 && index == displayLower -> fadeProgress.value
                    else -> 1f
                }

                key(index) {
                    Dot(
                        index = index,
                        currentIndex = currentIndex,
                        totalPages = totalPages,
                        activeColor = indicatorColor,
                        inactiveColor = inactiveIndicatorColor,
                        modifier = Modifier.graphicsLayer { alpha = dotAlpha },
                    )
                }
            }
        }
    }
}

private fun getWindowBounds(totalPages: Int, currentIndex: Int): Pair<Int, Int> {
    if (totalPages <= MAX_VISIBLE_DOTS) {
        return 0 to totalPages
    }
    val lowerBound = when {
        currentIndex <= 1 -> 0
        currentIndex >= totalPages - 2 -> totalPages - MAX_VISIBLE_DOTS
        else -> currentIndex - 2
    }
    val upperBound = min(lowerBound + MAX_VISIBLE_DOTS, totalPages)
    return lowerBound to upperBound
}

private fun getDotSize(index: Int, currentIndex: Int, totalPages: Int): DpSize {
    if (index == currentIndex) {
        return CURRENT_DOT_SIZE
    }
    if (totalPages <= MAX_VISIBLE_DOTS) {
        return NORMAL_DOT_SIZE
    }
    val params = DotSizeParams.create(index, currentIndex, totalPages)
    return params.calculateSize()
}

private class DotSizeParams private constructor(
    val posInWindow: Int,
    val currentPosInWindow: Int,
    val hiddenLeft: Int,
    val hiddenRight: Int,
    val distanceFromCurrent: Int,
) {
    private val lastPos = MAX_VISIBLE_DOTS - 1
    private val isCentered = currentPosInWindow == 2 && hiddenLeft >= 1 && hiddenRight >= 1

    fun calculateSize(): DpSize = when {
        isCentered -> getCenteredSize()
        hiddenRight >= 1 -> getRightEdgeSize()
        hiddenLeft >= 1 -> getLeftEdgeSize()
        else -> NORMAL_DOT_SIZE
    }

    private fun getCenteredSize(): DpSize = when (posInWindow) {
        0, lastPos -> HINT_DOT_SIZE
        else -> NORMAL_DOT_SIZE
    }

    private fun getRightEdgeSize(): DpSize {
        val isLastPos = posInWindow == lastPos
        val isSecondToLast = posInWindow == lastPos - 1
        val hasExtraHidden = hiddenRight >= MIN_HIDDEN_FOR_SMALL_DOT
        val isFarFromCurrent = distanceFromCurrent >= MIN_DISTANCE_FOR_SMALL_DOT
        val isModerateDistance = distanceFromCurrent >= MIN_DISTANCE_FOR_HINT_DOT

        return when {
            isLastPos && hasExtraHidden && isFarFromCurrent -> SMALL_DOT_SIZE
            isLastPos && isModerateDistance -> HINT_DOT_SIZE
            isSecondToLast && hasExtraHidden && isModerateDistance -> HINT_DOT_SIZE
            else -> NORMAL_DOT_SIZE
        }
    }

    private fun getLeftEdgeSize(): DpSize {
        val isFirstPos = posInWindow == 0
        val isSecondPos = posInWindow == 1
        val hasExtraHidden = hiddenLeft >= MIN_HIDDEN_FOR_SMALL_DOT
        val isFarFromCurrent = distanceFromCurrent >= MIN_DISTANCE_FOR_SMALL_DOT
        val isModerateDistance = distanceFromCurrent >= MIN_DISTANCE_FOR_HINT_DOT

        return when {
            isFirstPos && hasExtraHidden && isFarFromCurrent -> SMALL_DOT_SIZE
            isFirstPos && isModerateDistance -> HINT_DOT_SIZE
            isSecondPos && hasExtraHidden && isModerateDistance -> HINT_DOT_SIZE
            else -> NORMAL_DOT_SIZE
        }
    }

    companion object {
        fun create(index: Int, currentIndex: Int, totalPages: Int): DotSizeParams {
            val (windowStart, windowEnd) = getWindowBounds(totalPages, currentIndex)
            val posInWindow = index - windowStart
            val currentPosInWindow = currentIndex - windowStart
            return DotSizeParams(
                posInWindow = posInWindow,
                currentPosInWindow = currentPosInWindow,
                hiddenLeft = windowStart,
                hiddenRight = totalPages - windowEnd,
                distanceFromCurrent = abs(posInWindow - currentPosInWindow),
            )
        }
    }
}

@Composable
private fun Dot(
    index: Int,
    currentIndex: Int,
    totalPages: Int,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier,
) {
    val isActive = index == currentIndex
    val size = getDotSize(index, currentIndex, totalPages)

    val animSpec = tween<Dp>(ANIMATION_DURATION)
    val colorSpec = tween<Color>(ANIMATION_DURATION)

    val animatedWidth by animateDpAsState(size.width, animSpec, label = "w$index")
    val animatedHeight by animateDpAsState(size.height, animSpec, label = "h$index")
    val animatedColor by animateColorAsState(
        targetValue = if (isActive) activeColor else inactiveColor,
        animationSpec = colorSpec,
        label = "c$index",
    )

    val shape = RoundedCornerShape(animatedHeight / 2)

    Box(
        modifier = modifier
            .width(animatedWidth)
            .height(animatedHeight)
            .background(animatedColor, shape),
    )
}

@Preview(showBackground = true)
@Composable
private fun PagerIndicatorPreview() {
    TangemThemePreview {
        Column(
            Modifier
                .background(TangemTheme.colors.background.primary)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            listOf(0, 1, 2, 3, 4).forEach { page ->
                PagerIndicator(rememberPagerState(page) { 5 })
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PagerIndicator6ItemsPreview() {
    TangemThemePreview {
        Column(
            Modifier
                .background(TangemTheme.colors.background.primary)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            listOf(0, 1, 2, 3, 4, 5).forEach { page ->
                PagerIndicator(rememberPagerState(page) { 6 })
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PagerIndicator7ItemsPreview() {
    TangemThemePreview {
        Column(
            Modifier
                .background(TangemTheme.colors.background.primary)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            listOf(0, 1, 2, 3, 4, 5, 6).forEach { page ->
                PagerIndicator(rememberPagerState(page) { 7 })
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PagerIndicator10ItemsPreview() {
    TangemThemePreview {
        Column(
            Modifier
                .background(TangemTheme.colors.background.primary)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).forEach { page ->
                PagerIndicator(rememberPagerState(page) { 10 })
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PagerIndicatorSmallCountsPreview() {
    TangemThemePreview {
        Column(
            Modifier
                .background(TangemTheme.colors.background.primary)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PagerIndicator(rememberPagerState(0) { 1 })
            PagerIndicator(rememberPagerState(1) { 2 })
            PagerIndicator(rememberPagerState(1) { 3 })
        }
    }
}