package com.tangem.feature.wallet.presentation.wallet.ui.utils

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletEvent.DemonstrateWalletsScrollPreview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val VISIBLE_PART_OF_WALLET_CARD = 0.2f

internal fun LazyListState.demonstrateScrolling(
    coroutineScope: CoroutineScope,
    direction: DemonstrateWalletsScrollPreview.Direction,
) {
    coroutineScope.launch {
        animateScrollBy(
            value = calculateOffset(direction = direction, isReverse = false),
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        )
    }
        .invokeOnCompletion {
            coroutineScope.launch {
                animateScrollBy(
                    value = calculateOffset(direction = direction, isReverse = true),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                )
            }
        }
}

private fun LazyListState.calculateOffset(
    direction: DemonstrateWalletsScrollPreview.Direction,
    isReverse: Boolean,
): Float {
    val sign = when (direction) {
        DemonstrateWalletsScrollPreview.Direction.LEFT -> 1
        DemonstrateWalletsScrollPreview.Direction.RIGHT -> -1
    }.times(other = if (isReverse) -1 else 1)

    return layoutInfo.viewportSize.width.toFloat() * VISIBLE_PART_OF_WALLET_CARD * sign
}