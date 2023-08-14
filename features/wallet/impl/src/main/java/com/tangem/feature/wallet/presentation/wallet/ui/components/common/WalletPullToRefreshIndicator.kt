package com.tangem.feature.wallet.presentation.wallet.ui.components.common

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * "Pull to refresh" indicator
 *
 * @param isRefreshing indicator is currently refreshing or not
 * @param state        indicator state
 * @param modifier     modifier
 *
* [REDACTED_AUTHOR]
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun WalletPullToRefreshIndicator(
    isRefreshing: Boolean,
    state: PullRefreshState,
    modifier: Modifier = Modifier,
) {
    PullRefreshIndicator(refreshing = isRefreshing, state = state, modifier = modifier)
}
