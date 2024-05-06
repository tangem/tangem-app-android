package com.tangem.features.send.impl.presentation.analytics.utils

import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.features.send.impl.presentation.analytics.SelectedCurrencyType
import com.tangem.features.send.impl.presentation.analytics.SelectedFeeType
import com.tangem.features.send.impl.presentation.analytics.SendAnalyticEvents
import com.tangem.features.send.impl.presentation.analytics.SendScreenSource
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.SendUiStateType
import com.tangem.features.send.impl.presentation.state.StateRouter
import com.tangem.features.send.impl.presentation.state.fee.FeeSelectorState
import com.tangem.features.send.impl.presentation.state.fee.FeeType
import com.tangem.utils.Provider

internal class SendScreenAnalyticSender(
    private val stateRouterProvider: Provider<StateRouter>,
    private val currentStateProvider: Provider<SendUiState>,
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

    fun sendOnClose() {
        val routerState = stateRouterProvider().currentState.value
        val state = currentStateProvider()

        val (source, isValid) = when (routerState.type) {
            SendUiStateType.Recipient,
            SendUiStateType.EditRecipient,
            -> SendScreenSource.Address to (state.editRecipientState?.isPrimaryButtonEnabled ?: false)
            SendUiStateType.Amount,
            SendUiStateType.EditAmount,
            -> SendScreenSource.Amount to (state.editAmountState?.isPrimaryButtonEnabled ?: false)
            SendUiStateType.Fee,
            SendUiStateType.EditFee,
            -> SendScreenSource.Fee to (state.editFeeState?.isPrimaryButtonEnabled ?: false)
            else -> SendScreenSource.Confirm to true
        }

        analyticsEventHandler.send(
            SendAnalyticEvents.CloseButtonClicked(
                source = source,
                isFromSummary = routerState.isFromConfirmation,
                isValid = isValid,
            ),
        )
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
