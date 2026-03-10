package com.tangem.core.ui.components.containers.pullToRefresh

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import kotlin.math.ln

/**
 * A composable function that provides a pull-to-refresh container using Material3's PullToRefreshBox.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TangemPullToRefreshContainer(
    config: PullToRefreshConfig,
    modifier: Modifier = Modifier,
    indicatorModifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = config.isRefreshing,
        onRefresh = {
            config.onRefresh(PullToRefreshConfig.ShowRefreshState())
        },
        state = state,
        modifier = modifier,
        indicator = {
            Indicator(
                modifier = indicatorModifier.align(Alignment.TopCenter),
                isRefreshing = config.isRefreshing,
                state = state,
                containerColor = TangemTheme.colors.background.tertiary,
                color = TangemTheme.colors.text.primary1,
            )
        },
    ) {
        content()
    }
}

/**
 * A composable function that provides a pull-to-refresh container that slides the content down
 * to reveal a progress indicator, then slides it back up when refreshing completes.
 *
 * The indicator appears during the pull gesture (driven by drag distance) and remains visible
 * while refreshing is in progress.
 *
 * @param config            Pull-to-refresh configuration (isRefreshing, onRefresh).
 * @param modifier          Modifier applied to the outer container.
 * @param indicatorOffset   Additional offset for the indicator block position.
 * @param content           The content to display.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TangemPullToRefreshSlidingContainer(
    config: PullToRefreshConfig,
    modifier: Modifier = Modifier,
    state: PullToRefreshState = rememberPullToRefreshState(),
    indicatorOffset: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val indicatorSize = 24.dp
    val contentOffset = getPullToRefreshIndicatorOffset(
        pullToRefreshConfig = config,
        pullToRefreshState = state,
    )
    PullToRefreshBox(
        isRefreshing = config.isRefreshing,
        onRefresh = {
            config.onRefresh(PullToRefreshConfig.ShowRefreshState())
        },
        state = state,
        modifier = modifier,
        indicator = {},
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Content slides down
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = contentOffset),
            ) {
                content()
            }
            // Indicator block slides in from above
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(contentOffset)
                    .offset(y = indicatorOffset),
            ) {
                if (contentOffset > 0.dp) {
                    if (config.isRefreshing) {
                        // Indeterminate spinner while refreshing
                        CircularProgressIndicator(
                            modifier = Modifier.size(indicatorSize),
                            color = TangemTheme.colors2.graphic.neutral.primary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        // Determinate arc driven by pull fraction
                        CircularProgressIndicator(
                            progress = { state.distanceFraction.coerceAtLeast(0f).coerceIn(0f, 1f) },
                            modifier = Modifier.size(indicatorSize),
                            color = TangemTheme.colors2.graphic.neutral.primary,
                            trackColor = Color.Transparent,
                            strokeWidth = 2.dp,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Calculates the vertical offset for the pull-to-refresh indicator
 * based on the current pull state and refreshing status.
 */
@Composable
fun getPullToRefreshIndicatorOffset(
    pullToRefreshConfig: PullToRefreshConfig?,
    pullToRefreshState: PullToRefreshState,
): Dp {
    val indicatorBlockHeight = 56.dp
    val maxOverscroll = 24.dp

    val refreshingOffset by animateDpAsState(
        targetValue = if (pullToRefreshConfig?.isRefreshing == true) indicatorBlockHeight else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "SlidingContentOffset",
    )

    // Drag-driven offset with overscroll: linear up to indicatorBlockHeight,
    // then dampened logarithmic curve beyond for a rubber-band effect
    val fraction = pullToRefreshState.distanceFraction.coerceAtLeast(0f)
    val dragOffset = if (fraction <= 1f) {
        indicatorBlockHeight * fraction
    } else {
        val overscrollFraction = ln(1f + (fraction - 1f)) / ln(2f) // dampened curve
        indicatorBlockHeight + maxOverscroll * overscrollFraction.coerceAtMost(1f)
    }

    // Use the larger of the two so the transition from drag → refreshing is seamless
    return maxOf(dragOffset, refreshingOffset)
}

// region Preview
@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Preview(showBackground = true, widthDp = 360, heightDp = 720, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPullToRefreshContainer_Preview() {
    TangemThemePreview {
        TangemPullToRefreshContainer(
            config = PullToRefreshConfig(isRefreshing = true, {}),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TangemTheme.colors.background.secondary),
            )
        }
    }
}
// endregion

// region Preview
@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Preview(showBackground = true, widthDp = 360, heightDp = 720, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPullToRefreshSlidingContainer_Preview() {
    TangemThemePreviewRedesign {
        TangemPullToRefreshSlidingContainer(
            config = PullToRefreshConfig(isRefreshing = true, {}),
            indicatorOffset = 56.dp,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(TangemTheme.colors2.surface.level1),
            )
        }
    }
}
// endregion