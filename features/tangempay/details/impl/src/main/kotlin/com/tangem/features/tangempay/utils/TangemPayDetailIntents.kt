package com.tangem.features.tangempay.utils

import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig.ShowRefreshState
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanState

internal interface TangemPayDetailIntents {
    fun onContactSupportClicked()
    fun onRefreshSwipe(refreshState: ShowRefreshState)
    fun onRenewSession()
    fun onClickAddFunds()
    fun onClickWithdraw()
    fun onClickTermsAndLimits()
    fun onClickVisaBenefits()
    fun onClickCashback()
    fun onClickCurrentPlan(tariffPlan: TangemPayCustomerTariffPlan)
    fun onCancelTariffTransition(orderId: String)
    fun onCardClick(cardId: String)
    fun onAddCardClick(tariffState: TangemPayTariffPlanState?)
    fun onRemoveAccount()
}