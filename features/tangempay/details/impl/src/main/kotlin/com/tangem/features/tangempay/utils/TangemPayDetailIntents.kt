package com.tangem.features.tangempay.utils

import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig.ShowRefreshState

internal interface TangemPayDetailIntents {
    fun onRefreshSwipe(refreshState: ShowRefreshState)
    fun onClickAddFunds()
    fun onClickChangePin()
    fun onClickFreezeCard()
    fun onClickUnfreezeCard()
    fun onClickTermsAndLimits()
}