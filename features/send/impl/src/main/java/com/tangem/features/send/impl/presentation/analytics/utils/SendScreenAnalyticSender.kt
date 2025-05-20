package com.tangem.features.send.impl.presentation.analytics.utils

import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.send.impl.presentation.analytics.SelectedCurrencyType
import com.tangem.features.send.impl.presentation.analytics.SendAnalyticEvents
import com.tangem.features.send.impl.presentation.analytics.SendScreenSource
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.SendUiStateType
import com.tangem.features.send.impl.presentation.state.StateRouter
import com.tangem.features.send.impl.presentation.state.fee.FeeSelectorState
import com.tangem.features.send.impl.presentation.state.fee.FeeType
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import com.tangem.utils.Provider

internal class SendScreenAnalyticSender(
    private val stateRouterProvider: Provider<StateRouter>,
    private val currentStateProvider: Provider<SendUiState>,
    private val cryptoCurrencyProvider: Provider<CryptoCurrency>,
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
                val amountState = state.getAmountState(stateRouterProvider().isEditState) as? AmountState.Data ?: return
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
            -> SendScreenSource.Amount to state.editAmountState.isPrimaryButtonEnabled
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

    fun sendTransaction() {
        val state = currentStateProvider()
        val isEditState = stateRouterProvider().isEditState
        val cryptoCurrency = cryptoCurrencyProvider()
        val feeState = state.getFeeState(isEditState) ?: return
        val recipientState = state.getRecipientState(isEditState) ?: return

        val feeSelectorState = feeState.feeSelectorState as? FeeSelectorState.Content ?: return
        val feeType = getSendTransactionFeeType(feeSelectorState)
        analyticsEventHandler.send(
            SendAnalyticEvents.TransactionScreenOpened(
                token = cryptoCurrency.symbol,
                feeType = feeType,
            ),
        )
        analyticsEventHandler.send(
            Basic.TransactionSent(
                sentFrom = AnalyticsParam.TxSentFrom.Send(
                    blockchain = cryptoCurrency.network.name,
                    token = cryptoCurrency.symbol,
                    feeType = feeType,
                ),
                memoType = getSendTransactionMemoType(recipientState.memoTextField),
            ),
        )
    }

    private fun sendSelectedFeeAnalytics(feeSelectorState: FeeSelectorState.Content) {
        val type = getSendTransactionFeeType(feeSelectorState)
        analyticsEventHandler.send(SendAnalyticEvents.SelectedFee(type))
    }

    private fun getSendTransactionFeeType(feeSelectorState: FeeSelectorState.Content): AnalyticsParam.FeeType =
        when (feeSelectorState.fees) {
            is TransactionFee.Single -> AnalyticsParam.FeeType.Fixed
            is TransactionFee.Choosable -> when (feeSelectorState.selectedFee) {
                FeeType.Slow -> AnalyticsParam.FeeType.Min
                FeeType.Market -> AnalyticsParam.FeeType.Normal
                FeeType.Fast -> AnalyticsParam.FeeType.Max
                FeeType.Custom -> AnalyticsParam.FeeType.Custom
            }
        }

    private fun getSendTransactionMemoType(
        recipientMemo: SendTextField.RecipientMemo?,
    ): Basic.TransactionSent.MemoType {
        val memo = recipientMemo?.value
        return when {
            memo?.isBlank() == true -> Basic.TransactionSent.MemoType.Empty
            memo?.isNotBlank() == true -> Basic.TransactionSent.MemoType.Full
            else -> Basic.TransactionSent.MemoType.Null
        }
    }
}