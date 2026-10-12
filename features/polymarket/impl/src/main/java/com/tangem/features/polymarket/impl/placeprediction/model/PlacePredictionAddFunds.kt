package com.tangem.features.polymarket.impl.placeprediction.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.interactor.RefreshPredictionDepositRequisitesInteractor
import com.tangem.features.polymarket.impl.common.funding.PredictionFundingRequisites
import com.tangem.features.polymarket.impl.common.funding.RequisitesState
import com.tangem.features.polymarket.impl.main.addfunds.PolymarketAddFundsComponent
import com.tangem.features.polymarket.impl.main.addfunds.PolymarketFundingSheetConfig
import com.tangem.features.polymarket.impl.main.addfunds.PolymarketReceiveConfigFactory
import com.tangem.features.polymarket.impl.main.addfunds.dismissIfShown
import com.tangem.features.polymarket.impl.placeprediction.entity.TradingPermissionUM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

internal class PlacePredictionAddFunds(
    private val userWalletId: UserWalletId,
    private val activeStatus: StateFlow<PredictionAccountStatusValue.Active?>,
    private val router: Router,
    private val messageSender: UiMessageSender,
    refreshInteractor: RefreshPredictionDepositRequisitesInteractor,
    scope: CoroutineScope,
) : PolymarketAddFundsComponent.ModelCallback {

    val navigation = SlotNavigation<PolymarketFundingSheetConfig>()

    private val requisites = PredictionFundingRequisites(
        refreshInteractor = refreshInteractor,
        userWalletId = userWalletId,
        scope = scope,
    )

    fun isEnabled(permission: Flow<TradingPermissionUM?>): Flow<Boolean> = combine(
        flow = activeStatus,
        flow2 = requisites.state,
        flow3 = permission,
        transform = ::canAddFunds,
    ).distinctUntilChanged()

    fun prefetch() {
        requisites.prefetchOnce()
    }

    fun retryIfFailed() {
        requisites.retryIfFailed()
    }

    fun onAddFundsClick() {
        when (requisites.state.value) {
            is RequisitesState.Loading -> Unit
            is RequisitesState.Failed -> messageSender.send(
                DialogMessage(message = resourceReference(R.string.prediction_add_funds_error_description)),
            )
            is RequisitesState.Ready -> openAddFunds()
        }
    }

    override fun onSwapClick() {
        val collateral = activeStatus.value?.cryptoCurrencyStatus?.currency ?: return
        navigation.dismiss()
        router.push(
            AppRoute.Swap(
                userWalletId = userWalletId,
                fromCryptoCurrency = collateral,
                fromCurrencyPosition = AppRoute.Swap.CurrencyPosition.TO,
                screenSource = AnalyticsParam.ScreensSources.Predictions.value,
                accountFlow = AppRoute.Swap.AccountFlow.PredictionTopUp,
            ),
        )
    }

    override fun onReceiveClick() {
        val collateral = activeStatus.value?.cryptoCurrencyStatus?.currency ?: return
        val target = requisites.target ?: return

        navigation.activate(
            PolymarketFundingSheetConfig.Receive(
                config = PolymarketReceiveConfigFactory.create(
                    cryptoCurrency = collateral,
                    userWalletId = userWalletId,
                    target = target,
                ),
            ),
        )
    }

    override fun onDismissAddFunds() {
        navigation.dismissIfShown(PolymarketFundingSheetConfig.AddFunds)
    }

    private fun openAddFunds() {
        if (activeStatus.value?.cryptoCurrencyStatus == null) {
            messageSender.send(SnackbarMessage(message = resourceReference(R.string.common_unknown_error)))
            return
        }

        navigation.activate(PolymarketFundingSheetConfig.AddFunds)
    }

    private fun canAddFunds(
        status: PredictionAccountStatusValue.Active?,
        requisites: RequisitesState,
        permission: TradingPermissionUM?,
    ): Boolean = permission != null && permission != TradingPermissionUM.Restricted && when (requisites) {
        RequisitesState.Loading -> false
        RequisitesState.Failed -> status != null
        is RequisitesState.Ready -> status?.cryptoCurrencyStatus != null
    }
}