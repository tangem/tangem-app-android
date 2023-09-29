package com.tangem.feature.wallet.presentation.wallet.ui.utils

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import kotlin.math.sin

private val minOffset = 0.dp
private val maxOffset = 40.dp

private const val MIN_ALPHA = 0f
private const val MAX_ALPHA = 1f

private const val PI = Math.PI.toFloat()
private const val HALF_PI = PI / 2
private const val DOUBLE_PI = 2 * PI

private const val Y_AXIS_OFFSET = 0.5f
private const val AMPLITUDE_MULTIPLIER = 0.5f

/**
 * Modifier extension for setting [absoluteOffset] and [alpha] animations of wallet change
 *
 * @param lazyListState lazy list state
 *
[REDACTED_AUTHOR]
 */
internal fun Modifier.changeWalletAnimator(lazyListState: LazyListState) = composed {
    val offset by remember(lazyListState) { derivedStateOf { lazyListState.firstVisibleItemScrollOffset } }

    val size by remember(lazyListState) {
        derivedStateOf { lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1 }
    }

    val contentOffsetY by rememberOffsetY(offset = offset, size = size)
    val contentAlpha by rememberAlpha(offset = offset, size = size)

    val animatedOffsetY by animateDpAsState(targetValue = contentOffsetY, label = "offsetY")
    val animatedContentAlpha by animateFloatAsState(targetValue = contentAlpha, label = "alpha")

    this
        .absoluteOffset(y = animatedOffsetY)
        .alpha(alpha = animatedContentAlpha)
}

/**
 * y = ANIMATION_MAX_OFFSET_IN_DP.dp * sin(π / size * offset)
 *
 * @see <a href="https://www.wolframalpha.com/input?i=plot%2820+*+sin%28pi+%2F+100+*+x%29%2C0..100%29">Graphic</a>
 */
@Composable
private fun rememberOffsetY(offset: Int, size: Int): State<Dp> {
    return remember(offset) {
        derivedStateOf {
            val y = maxOffset * sin(x = PI / size * offset)
            y.coerceIn(minimumValue = minOffset, maximumValue = maxOffset)
        }
    }
}

/**
 * y = 0.5 * sin(2 * pi / size * offset + pi / 2) + 0.5
 *
 * @see <a href="https://www.wolframalpha.com/input?i=plot%280.5+*+sin%282pi+%2F+100+*+x+%2B+pi%2F2%29+%2B+0.5%2C0..100%29"
 * >Graphic</a>
 */
@Composable
private fun rememberAlpha(offset: Int, size: Int): State<Float> {
    return remember(offset) {
        derivedStateOf {
            val y = AMPLITUDE_MULTIPLIER * sin(x = DOUBLE_PI / size * offset + HALF_PI) + Y_AXIS_OFFSET
            y.coerceIn(MIN_ALPHA, MAX_ALPHA)
        }
    }
}