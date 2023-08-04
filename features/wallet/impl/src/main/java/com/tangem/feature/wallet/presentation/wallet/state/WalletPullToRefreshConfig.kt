package com.tangem.feature.wallet.presentation.wallet.state

/**
 * Wallet screen top bar config
 *
 * @property isRefreshing     state is indicator visible
 * @property onRefresh lambda be invoked when pulled to refresh
 */
data class WalletPullToRefreshConfig(val isRefreshing: Boolean, val onRefresh: () -> Unit)
