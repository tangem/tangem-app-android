package com.tangem.features.tangempay.utils

import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig.ShowRefreshState

internal interface TangemPayDetailIntents {
    fun onContactSupportClicked()
    fun onRefreshSwipe(refreshState: ShowRefreshState)
    fun onRenewSession()
    fun onClickAddFunds()
    fun onClickWithdraw()
    fun onClickTermsAndLimits()
    fun onCardClick()
    fun onAddCardClick()
    fun onRemoveAccount()
}