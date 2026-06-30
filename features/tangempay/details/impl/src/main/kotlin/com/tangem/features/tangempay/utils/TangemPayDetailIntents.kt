package com.tangem.features.tangempay.utils

import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig.ShowRefreshState
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan

internal interface TangemPayDetailIntents {
    fun onContactSupportClicked()
    fun onRefreshSwipe(refreshState: ShowRefreshState)
    fun onRenewSession()
    fun onClickAddFunds()
    fun onClickWithdraw()
    fun onClickTermsAndLimits()
    fun onClickCurrentPlan(tariffPlan: TangemPayCustomerTariffPlan)
    fun onCardClick(cardId: String)
    fun onAddCardClick()
    fun onRemoveAccount()
}