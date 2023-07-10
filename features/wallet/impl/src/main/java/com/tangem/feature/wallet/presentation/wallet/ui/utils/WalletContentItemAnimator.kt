package com.tangem.feature.wallet.presentation.wallet.ui.utils

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

private const val ANIMATION_MAX_OFFSET_IN_DP = 80
private const val ANIMATION_MAX_ALPHA = 1f

/**
 * Modifier extension for setting [absoluteOffset] and [alpha] animations of wallet change
 *
 * @param lazyListState lazy list state
 *
[REDACTED_AUTHOR]
 */
internal fun Modifier.changeWalletAnimator(lazyListState: LazyListState) = composed {
    val walletItemOffset by remember(lazyListState) { derivedStateOf { lazyListState.firstVisibleItemScrollOffset } }

    val walletItemSize by remember(lazyListState) {
        derivedStateOf { lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1 }
    }

    val walletHalfItemSize by remember {
        derivedStateOf { walletItemSize / 2 }
    }

    val contentOffsetY by remember {
        derivedStateOf {
            /*
             * Until [walletItemOffset] is less than [walletHalfItemSize],
             * then content offset animation has positive value (downward movement).
             * Otherwise, it has negative value (upward movement).
             */
            val position = if (walletItemOffset < walletHalfItemSize) {
                walletItemOffset
            } else {
                walletItemSize - walletItemOffset
            }

            ANIMATION_MAX_OFFSET_IN_DP.dp / walletItemSize * position
        }
    }

    val contentAlpha by remember {
        derivedStateOf {
            /*
             * Until [walletItemOffset] is less than [walletHalfItemSize],
             * then content alpha animation has negative value (fade out).
             * Otherwise, it has positive value (fade in).
             */
            val position = if (walletItemOffset < walletHalfItemSize) {
                walletHalfItemSize - walletItemOffset
            } else {
                walletItemOffset - walletHalfItemSize
            }

            ANIMATION_MAX_ALPHA / walletHalfItemSize * position
        }
    }

    val animatedOffsetY by animateDpAsState(targetValue = contentOffsetY)
    val animatedContentAlpha by animateFloatAsState(targetValue = contentAlpha)

    this
        .absoluteOffset(y = animatedOffsetY)
        .alpha(alpha = animatedContentAlpha)
}