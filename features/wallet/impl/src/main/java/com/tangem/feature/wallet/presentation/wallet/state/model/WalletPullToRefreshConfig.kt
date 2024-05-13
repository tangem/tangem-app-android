package com.tangem.feature.wallet.presentation.wallet.state.model

/**
 * Wallet screen top bar config
 *
 * @property isRefreshing     state is indicator visible
 * @property onRefresh lambda be invoked when pulled to refresh
 */
data class WalletPullToRefreshConfig(val isRefreshing: Boolean, val onRefresh: (ShowRefreshState) -> Unit) {

    @JvmInline
    value class ShowRefreshState(
        val value: Boolean,
    )
}
