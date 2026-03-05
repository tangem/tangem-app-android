package com.tangem.core.ui.components.pager

import androidx.compose.animation.animateColorAsState
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
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import dev.chrisbanes.haze.HazeStyle
import kotlin.math.abs
import kotlin.math.roundToInt

internal const val ANIMATION_DURATION = 300
internal const val MAX_VISIBLE_DOTS = 5
internal val SPACING = 4.dp
internal val HINT_DOT_SIZE = DpSize(6.dp, 6.dp)
private const val MIN_HIDDEN_FOR_SMALL_DOT = 2
private const val MIN_DISTANCE_FOR_SMALL_DOT = 3
private const val MIN_DISTANCE_FOR_HINT_DOT = 2
private val BACKGROUND_SIZE = DpSize(92.dp, 32.dp)
private val CURRENT_DOT_SIZE = DpSize(16.dp, 8.dp)
private val NORMAL_DOT_SIZE = DpSize(8.dp, 8.dp)
private val SMALL_DOT_SIZE = DpSize(4.dp, 4.dp)

@Composable
fun PagerIndicator(pagerState: PagerState, modifier: Modifier = Modifier) {
    if (LocalRedesignEnabled.current) {
        PagerIndicatorV2(pagerState, modifier)
    } else {
        PagerIndicatorV1(pagerState, modifier)
    }
}

@Composable
private fun PagerIndicatorV1(pagerState: PagerState, modifier: Modifier = Modifier) {
    val colors = PagerIndicatorColors(
        active = TangemTheme.colors.control.key,
        inactive = TangemTheme.colors.text.tertiary,
        overlay = TangemTheme.colors.overlay.secondary,
    )
    PagerIndicatorContent(
        pagerState = pagerState,
        colors = colors,
        modifier = modifier,
    )
}

@Composable
private fun PagerIndicatorV2(pagerState: PagerState, modifier: Modifier = Modifier) {
    val colors = PagerIndicatorColors(
        active = TangemTheme.colors2.graphic.neutral.primary,
        inactive = TangemTheme.colors2.graphic.neutral.tertiary,
        overlay = TangemTheme.colors2.tabs.backgroundSecondary.copy(alpha = .1f),
    )
    PagerIndicatorContent(
        pagerState = pagerState,
        colors = colors,
        modifier = modifier,
        boxModifier = Modifier.hazeEffectTangem(style = HazeStyle(blurRadius = 22.dp, tint = null)),
    )
}

@Composable
private fun rememberPagerIndicatorAnimationState(pagerState: PagerState): PagerIndicatorAnimationState {
    val density = LocalDensity.current
    return remember(pagerState.pageCount, density) {
        PagerIndicatorAnimationState(pagerState.pageCount, pagerState.currentPage, density)
    }
}

@Suppress("LongParameterList")
private fun calculateDotAlpha(
    isSliding: Boolean,
    slideDirection: Int,
    index: Int,
    displayLower: Int,
    displayUpper: Int,
    fadeProgress: Float,
): Float {
    return when {
        !isSliding -> 1f
        slideDirection > 0 && index == displayLower -> 1f - fadeProgress
        slideDirection > 0 && index == displayUpper - 1 -> fadeProgress
        slideDirection < 0 && index == displayUpper - 1 -> 1f - fadeProgress
        slideDirection < 0 && index == displayLower -> fadeProgress
        else -> 1f
    }
}

@Composable
private fun PagerIndicatorContent(
    pagerState: PagerState,
    colors: PagerIndicatorColors,
    modifier: Modifier = Modifier,
    boxModifier: Modifier = Modifier,
) {
    val totalPages = pagerState.pageCount
    if (totalPages == 0) return

    val animState = rememberPagerIndicatorAnimationState(pagerState)
    val (targetLower, targetUpper) = getWindowBounds(pagerState.pageCount, pagerState.currentPage)

    LaunchedEffect(targetLower) {
        animState.onBoundsChange(this, targetLower, targetUpper)
    }

    val visibleIndices = (animState.displayLower until animState.displayUpper).toList()

    Box(
        modifier = modifier
            .width(BACKGROUND_SIZE.width)
            .height(BACKGROUND_SIZE.height)
            .background(
                color = colors.overlay,
                shape = CircleShape,
            )
            .clip(CircleShape)
            .then(boxModifier),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.offset {
                IntOffset(animState.slideOffset.value.roundToInt(), 0)
            },
            horizontalArrangement = Arrangement.spacedBy(SPACING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            visibleIndices.forEach { index ->
                val dotAlpha = calculateDotAlpha(
                    isSliding = animState.isSliding,
                    slideDirection = animState.slideDirection,
                    index = index,
                    displayLower = animState.displayLower,
                    displayUpper = animState.displayUpper,
                    fadeProgress = animState.fadeProgress.value,
                )

                key(index) {
                    Dot(
                        index = index,
                        currentIndex = pagerState.currentPage,
                        totalPages = totalPages,
                        activeColor = colors.active,
                        inactiveColor = colors.inactive,
                        modifier = Modifier.graphicsLayer { alpha = dotAlpha },
                    )
                }
            }
        }
    }
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

@Immutable
private data class PagerIndicatorColors(
    val active: Color,
    val inactive: Color,
    val overlay: Color,
)

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

@Preview(showBackground = true)
@Composable
private fun PagerIndicatorPreviewV1() {
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
private fun PagerIndicatorPreviewV2() {
    TangemThemePreviewRedesign {
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