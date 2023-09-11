package com.tangem.feature.referral.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme

internal enum class CornersToRound {

    ALL_4,
    TOP_2,
    BOTTOM_2,
    ZERO,
    ;

    @Suppress("TopLevelComposableFunctions")
    @Composable
    fun getShape(): RoundedCornerShape {
        val radius = TangemTheme.dimens.radius12
        return when (this) {
            ALL_4 -> RoundedCornerShape(radius)
            TOP_2 -> RoundedCornerShape(topStart = radius, topEnd = radius)
            BOTTOM_2 -> RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
            ZERO -> RoundedCornerShape(0.dp)
        }
    }
}