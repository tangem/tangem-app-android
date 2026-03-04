package com.tangem.feature.wallet.presentation.wallet.ui.components.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.TangemPagerIndicator
import com.tangem.core.ui.ds.topbar.collapsing.TangemCollapsingAppBarBehavior

private const val MIN_SCALE = 0.75f
private const val MAX_SCALE = 1f

@Composable
internal fun WalletPagerIndicator(pagerState: PagerState, behavior: TangemCollapsingAppBarBehavior) {
    val collapsedFraction = behavior.state.collapsedFraction
    val alpha = MAX_SCALE - collapsedFraction
    val scale = alpha.coerceIn(MIN_SCALE, MAX_SCALE)

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleY = scale
                translationY = behavior.state.heightOffset
            }
            .fillMaxWidth()
            .height(
                with(LocalDensity.current) {
                    behavior.state.heightOffsetLimit.toDp().unaryMinus()
                },
            )
            .alpha(alpha),
    ) {
        TangemPagerIndicator(
            pagerState = pagerState,
            modifier = Modifier
                .padding(top = 248.dp)
                .scale(scaleY = 1f, scaleX = scale)
                .fillMaxWidth(),
        )
    }
}