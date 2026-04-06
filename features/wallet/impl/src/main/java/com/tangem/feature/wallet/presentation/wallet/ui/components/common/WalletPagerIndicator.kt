package com.tangem.feature.wallet.presentation.wallet.ui.components.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.containers.pullToRefresh.getPullToRefreshIndicatorOffset
import com.tangem.core.ui.ds.TangemPagerIndicator
import com.tangem.core.ui.ds.topbar.collapsing.TangemCollapsingAppBarBehavior

private const val MIN_SCALE = 0.75f
private const val MAX_SCALE = 1f
private const val WALLET_INDICATOR_OFFSET = 0.6f

@Composable
internal fun WalletPagerIndicator(
    pagerState: PagerState,
    behavior: TangemCollapsingAppBarBehavior,
    pullToRefreshConfig: PullToRefreshConfig?,
    pullToRefreshState: PullToRefreshState,
) {
    val collapsedFraction = behavior.state.collapsedFraction
    val alpha = MAX_SCALE - collapsedFraction
    val scale = alpha.coerceIn(MIN_SCALE, MAX_SCALE)
    val height = with(LocalDensity.current) {
        behavior.state.heightOffsetLimit.toDp().unaryMinus()
    }

    val contentOffset = getPullToRefreshIndicatorOffset(
        pullToRefreshConfig = pullToRefreshConfig,
        pullToRefreshState = pullToRefreshState,
    )
    val padding = height * WALLET_INDICATOR_OFFSET

    AnimatedVisibility(
        visible = pagerState.pageCount > 1,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleY = scale
                    translationY = behavior.state.heightOffset + contentOffset.toPx()
                }
                .fillMaxWidth()
                .height(height)
                .alpha(alpha),
        ) {
            TangemPagerIndicator(
                pagerState = pagerState,
                modifier = Modifier
                    .padding(top = padding)
                    .scale(scaleY = 1f, scaleX = scale)
                    .fillMaxWidth(),
            )
        }
    }
}