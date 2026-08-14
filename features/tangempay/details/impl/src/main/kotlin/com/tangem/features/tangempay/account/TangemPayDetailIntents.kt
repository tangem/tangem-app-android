package com.tangem.features.tangempay.account

import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig.ShowRefreshState
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
    fun onClickCurrentPlan(tariffPlan: TangemPayTariffPlanState)
    fun onCardClick(cardId: String)
    fun onAddCardClick(tariffState: TangemPayTariffPlanState?)
    fun onRemoveAccount()
}