package com.tangem.features.tangempay.utils

import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig.ShowRefreshState
import com.tangem.domain.pay.TangemPayDetailsConfig

internal interface TangemPayDetailIntents {
    fun onContactSupportClicked()
    fun onRefreshSwipe(refreshState: ShowRefreshState)
    fun onClickAddFunds()
    fun onClickWithdraw()
    fun onClickTermsAndLimits()
    fun onCardClick(config: TangemPayDetailsConfig)
    fun onAddCardClick()
}