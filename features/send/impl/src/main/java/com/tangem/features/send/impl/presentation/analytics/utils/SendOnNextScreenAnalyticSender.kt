package com.tangem.features.send.impl.presentation.analytics.utils

import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.features.send.impl.presentation.analytics.SelectedCurrencyType
import com.tangem.features.send.impl.presentation.analytics.SelectedFeeType
import com.tangem.features.send.impl.presentation.analytics.SendAnalyticEvents
import com.tangem.features.send.impl.presentation.state.*
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.StateRouter
import com.tangem.features.send.impl.presentation.state.fee.FeeSelectorState
import com.tangem.features.send.impl.presentation.state.fee.FeeType
import com.tangem.utils.Provider

internal class SendOnNextScreenAnalyticSender(
    private val stateRouterProvider: Provider<StateRouter>,
    private val analyticsEventHandler: AnalyticsEventHandler,
) {
    fun send(prevScreen: SendUiStateType, state: SendUiState) {
        when (prevScreen) {
            SendUiStateType.Fee -> {
                val feeState = state.getFeeState(stateRouterProvider().isEditState) ?: return
                val feeSelectorState = feeState.feeSelectorState as? FeeSelectorState.Content
                feeSelectorState?.selectedFee?.let { selectedFee ->
                    val isCustomFeeEdited = feeState.fee?.amount?.value != feeSelectorState.fees.normal.amount.value
                    if (selectedFee == FeeType.Custom && isCustomFeeEdited) {
                        analyticsEventHandler.send(SendAnalyticEvents.GasPriceInserter)
                    }
                    sendSelectedFeeAnalytics(feeSelectorState)
                }
            }
            SendUiStateType.Amount -> {
                val amountState = state.getAmountState(stateRouterProvider().isEditState) ?: return
                val isFiatSelected = amountState.amountTextField.isFiatValue
                val selectedCurrency = if (!isFiatSelected) {
                    SelectedCurrencyType.Token
                } else {
                    SelectedCurrencyType.AppCurrency
                }
                analyticsEventHandler.send(
                    SendAnalyticEvents.SelectedCurrency(selectedCurrency),
                )
            }
            else -> Unit
        }
    }

    private fun sendSelectedFeeAnalytics(feeSelectorState: FeeSelectorState.Content) {
        val type = when (feeSelectorState.fees) {
            is TransactionFee.Single -> SelectedFeeType.Fixed
            is TransactionFee.Choosable -> when (feeSelectorState.selectedFee) {
                FeeType.Slow -> SelectedFeeType.Min
                FeeType.Market -> SelectedFeeType.Normal
                FeeType.Fast -> SelectedFeeType.Max
                FeeType.Custom -> SelectedFeeType.Custom
            }
        }
        analyticsEventHandler.send(SendAnalyticEvents.SelectedFee(type))
    }
}
