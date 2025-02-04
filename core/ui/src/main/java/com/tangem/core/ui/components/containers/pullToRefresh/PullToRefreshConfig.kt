package com.tangem.core.ui.components.containers.pullToRefresh

/**
 * Pull to refresh config data
 *
 * @property isRefreshing state is indicator visible
 * @property onRefresh lambda be invoked when pulled to refresh
 */
data class PullToRefreshConfig(val isRefreshing: Boolean, val onRefresh: (ShowRefreshState) -> Unit) {

    @JvmInline
    value class ShowRefreshState(
        val value: Boolean = true,
    )
}