package com.tangem.features.send.impl.presentation.analytics.utils

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.features.send.impl.presentation.analytics.SelectedCurrencyType
import com.tangem.features.send.impl.presentation.analytics.SendAnalyticEvents
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.SendUiStateType
import com.tangem.features.send.impl.presentation.state.fee.FeeSelectorState
import com.tangem.features.send.impl.presentation.state.fee.FeeType

internal class SendOnNextScreenAnalyticSender(
    private val analyticsEventHandler: AnalyticsEventHandler,
) {
    fun send(prevScreen: SendUiStateType, state: SendUiState) {
        when (prevScreen) {
            SendUiStateType.Fee -> {
                val feeState = state.feeState ?: return
                val feeSelectorState = feeState.feeSelectorState as? FeeSelectorState.Content
                feeSelectorState?.selectedFee?.let { selectedFee ->
                    val isCustomFeeEdited = feeState.fee?.amount?.value != feeSelectorState.fees.normal.amount.value
                    if (selectedFee == FeeType.Custom && isCustomFeeEdited) {
                        analyticsEventHandler.send(SendAnalyticEvents.GasPriceInserter)
                    }
                    analyticsEventHandler.send(SendAnalyticEvents.SelectedFee(selectedFee.name))
                }
                if (feeState.isSubtract) {
                    analyticsEventHandler.send(SendAnalyticEvents.SubtractFromAmount)
                }
            }
            SendUiStateType.Amount -> {
                val isFiatSelected = state.amountState?.amountTextField?.isFiatValue ?: return
                val selectedCurrency = if (isFiatSelected) {
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
}
