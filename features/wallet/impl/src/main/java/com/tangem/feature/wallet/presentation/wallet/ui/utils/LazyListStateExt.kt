package com.tangem.feature.wallet.presentation.wallet.ui.utils

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState

internal suspend fun LazyListState.animateScrollByIndex(prevIndex: Int, newIndex: Int) {
    animateScrollBy(
        value = calculateOffset(layoutInfo, prevIndex, newIndex),
        animationSpec = tween(durationMillis = 1000),
    )
}

private fun calculateOffset(layoutInfo: LazyListLayoutInfo, prevIndex: Int, newIndex: Int): Float {
    return layoutInfo.viewportSize.width.times(other = newIndex - prevIndex).toFloat()
}