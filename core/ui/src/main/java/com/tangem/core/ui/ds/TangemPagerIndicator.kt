package com.tangem.core.ui.ds

import android.content.res.Configuration
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import kotlinx.coroutines.CoroutineScope
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

private val SPACING = 8.dp
private val DOT_SLOT_SIZE = 8.dp
private val NORMAL_DOT_SIZE = DpSize(8.dp, 8.dp)
private val HINT_DOT_SIZE = DpSize(6.dp, 6.dp)
private val SMALL_DOT_SIZE = DpSize(4.dp, 4.dp)

/**
 * A pager indicator that adapts to the number of pages and the current page index.
 *
 * [Figma]("https://www.figma.com/design/RU7AIgwHtGdMfy83T5UOoR/Core-Library?node-id=8452-16489&m=dev")
 *
 * For 5 or fewer pages, it shows all dots with the current page highlighted.
 * For more than 5 pages, it shows a sliding window of 5 dots with size and opacity indicating position.
 *
 * @param pagerState             state of the pager to observe
 * @param modifier               modifier for styling
 * @param colors                 colors of indicators(active/inactive) and overlay
 */
@Composable
fun TangemPagerIndicator(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    colors: PagerIndicatorColors = TangemPagerIndicatorColors,
) {
    val totalPages = pagerState.pageCount
    val currentIndex = pagerState.currentPage

    if (totalPages == 0) return

    val animState = rememberPagerIndicatorAnimationState(pagerState)
    val (targetLower, targetUpper) = getWindowBounds(totalPages, currentIndex)

    LaunchedEffect(targetLower) {
        animState.onBoundsChange(this, targetLower, targetUpper)
    }

    val visibleIndices = (animState.displayLower until animState.displayUpper).toList()

    Box(
        modifier = modifier
            .width(getSize(totalPages))
            .conditionalCompose(colors.overlay != null) {
                colors.overlay?.let { overlay ->
                    background(
                        color = overlay,
                        shape = CircleShape,
                    )
                } ?: this
            }
            .padding(TangemTheme.dimens2.x3),
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
                        currentIndex = currentIndex,
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

@Stable
private class PagerIndicatorAnimationState(
    private val totalPages: Int,
    initialCurrentPage: Int,
    private val density: Density,
) {
    var displayLower by mutableIntStateOf(0)
        private set
    var displayUpper by mutableIntStateOf(0)
        private set

    val slideOffset = Animatable(0f)
    var isSliding by mutableStateOf(false)
        private set
    var slideDirection by mutableIntStateOf(0)
        private set
    val fadeProgress = Animatable(0f)
    private var fadeJob: Job? = null
    private var prevTargetLower: Int = 0

    init {
        val (lower, upper) = getWindowBounds(totalPages, initialCurrentPage)
        displayLower = lower
        displayUpper = upper
        prevTargetLower = lower
    }

    suspend fun onBoundsChange(scope: CoroutineScope, targetLower: Int, targetUpper: Int) {
        if (targetLower == prevTargetLower || totalPages <= MAX_VISIBLE_DOTS) {
            return
        }

        fadeJob?.cancel()
        slideOffset.stop()
        fadeProgress.stop()

        val dir = if (targetLower > prevTargetLower) 1 else -1
        val dotSlot = with(density) { (DOT_SLOT_SIZE + SPACING).toPx() }

        isSliding = true
        slideDirection = dir
        fadeProgress.snapTo(0f)

        if (dir > 0) {
            displayUpper = targetUpper
            slideOffset.snapTo(0f)
        } else {
            displayLower = targetLower
            displayUpper = prevTargetLower + MAX_VISIBLE_DOTS
            slideOffset.snapTo(-dotSlot)
        }

        prevTargetLower = targetLower

        fadeJob = scope.launch {
            fadeProgress.animateTo(1f, tween(ANIMATION_DURATION))
        }
        slideOffset.animateTo(
            if (dir > 0) -dotSlot else 0f,
            tween(ANIMATION_DURATION),
        )

        displayLower = targetLower
        displayUpper = targetUpper
        slideOffset.snapTo(0f)
        isSliding = false
        slideDirection = 0
    }
}

@Suppress("MagicNumber")
@Composable
private fun getSize(pageCount: Int): Dp {
    return when (pageCount) {
        0 -> TangemTheme.dimens2.x0
        1 -> TangemTheme.dimens2.x8
        2 -> TangemTheme.dimens2.x12
        3 -> TangemTheme.dimens2.x16
        4 -> TangemTheme.dimens2.x20
        else -> TangemTheme.dimens2.x24
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

    Box(modifier.size(8.dp)) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(animatedWidth)
                .height(animatedHeight)
                .background(animatedColor, shape),
        )
    }
}

val TangemPagerIndicatorColors: PagerIndicatorColors
    @Composable
    @ReadOnlyComposable
    get() = PagerIndicatorColors(
        active = TangemTheme.colors2.graphic.neutral.primary,
        inactive = TangemTheme.colors2.graphic.neutral.tertiary,
        overlay = null,
    )

@Immutable
data class PagerIndicatorColors(
    val active: Color,
    val inactive: Color,
    val overlay: Color?,
)

// region Preview
@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TangemPagerIndicator_Preview(@PreviewParameter(TangemPagerIndicatorPreviewProvider::class) params: Int) {
    TangemThemePreviewRedesign {
        Column(
            Modifier
                .background(TangemTheme.colors2.surface.level1)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(params) { index ->
                TangemPagerIndicator(
                    pagerState = rememberPagerState(index) { params },
                    colors = TangemPagerIndicatorColors.copy(
                        overlay = if (index % 2 == 0) {
                            TangemTheme.colors2.tabs.backgroundSecondary
                        } else {
                            null
                        },
                    ),
                )
            }
        }
    }
}

private class TangemPagerIndicatorPreviewProvider : PreviewParameterProvider<Int> {
    override val values: Sequence<Int>
        get() = sequenceOf(
            1,
            2,
            3,
            5,
            6,
            7,
            10,
        )
}
// endregion