package com.tangem.tap.features.send.redux.middlewares

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionError
import com.tangem.common.extensions.isZero
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.TapError
import com.tangem.tap.features.send.redux.*
import com.tangem.tap.features.send.redux.states.MainCurrencyType
import org.rekotlin.Action
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
class AmountMiddleware {

    fun handle(action: AmountActionUi, appState: AppState?, dispatch: (Action) -> Unit) {
        when (action) {
            is AmountActionUi.HandleUserInput -> handleUserInput(action.data, appState, dispatch)
            is AmountActionUi.CheckAmountToSend -> checkAmountToSend(appState, dispatch)
            is AmountActionUi.SetMaxAmount -> setMaxAmount(appState, dispatch)
            is AmountActionUi.ToggleMainCurrency -> toggleMainCurrency(appState, dispatch)
            is AmountActionUi.SetMainCurrency -> return
        }
    }

    private fun handleUserInput(data: String, appState: AppState?, dispatch: (Action) -> Unit) {
        val sendState = appState?.sendState ?: return
        val amountState = sendState.amountState

        val data = if (data == ".") "0.0" else data
        val inputValue = when {
            data.isEmpty() || data == "0" -> BigDecimal.ZERO
            else -> BigDecimal(data)
        }
        if (inputValue.isZero() && amountState.amountToSendCrypto.isZero()) return

        val inputValueCrypto = if (amountState.mainCurrency.type == MainCurrencyType.CRYPTO) {
            inputValue
        } else {
            sendState.convertFiatToExtractCrypto(inputValue)
        }

        dispatch(AmountAction.SetAmount(inputValueCrypto, true))
        dispatch(FeeAction.RequestFee)
    }

    private fun checkAmountToSend(appState: AppState?, dispatch: (Action) -> Unit) {
        val sendState = appState?.sendState ?: return
        val walletManager = sendState.walletManager ?: return
        val typedAmount = sendState.amountState.amountToExtract ?: return

        val inputCrypto = sendState.amountState.amountToSendCrypto
        if (sendState.amountState.viewAmountValue.value == "0" && inputCrypto.isZero()) {
            dispatch(ReceiptAction.RefreshReceipt)
            dispatch(SendAction.ChangeSendButtonState(sendState.getButtonState()))
            return
        }

        val amountToSend = Amount(typedAmount, sendState.getTotalAmountToSend(inputCrypto))
        val transactionErrors = walletManager.validateTransaction(amountToSend, sendState.feeState.currentFee)
        if (transactionErrors.isEmpty()) {
            dispatch(AmountAction.SetAmountError(null))
        } else {
            val tapErrors = transactionErrors.map {
                when (it) {
                    TransactionError.AmountExceedsBalance -> TapError.AmountExceedsBalance
                    TransactionError.FeeExceedsBalance -> TapError.FeeExceedsBalance
                    TransactionError.TotalExceedsBalance -> TapError.TotalExceedsBalance
                    TransactionError.InvalidAmountValue -> TapError.InvalidAmountValue
                    TransactionError.InvalidFeeValue -> TapError.InvalidFeeValue
                    TransactionError.DustAmount -> TapError.DustAmount
                    TransactionError.DustChange -> TapError.DustChange
                    else -> TapError.UnknownError
                }
            }
            val error = TapError.ValidateTransactionErrors(tapErrors) { it.joinToString("\r\n") }
            dispatch(AmountAction.SetAmountError(error))
        }
        dispatch(ReceiptAction.RefreshReceipt)
        dispatch(SendAction.ChangeSendButtonState(sendState.getButtonState()))
    }

    private fun setMaxAmount(appState: AppState?, dispatch: (Action) -> Unit) {
        val amountState = appState?.sendState?.amountState ?: return

        dispatch(AmountAction.SetAmount(amountState.balanceCrypto, false))
        dispatch(FeeAction.RequestFee)
    }

    private fun toggleMainCurrency(appState: AppState?, dispatch: (Action) -> Unit) {
        val amountState = appState?.sendState?.amountState ?: return
        if (!appState.sendState.coinIsConvertible()) return

        val type = if (amountState.mainCurrency.type == MainCurrencyType.FIAT) {
            MainCurrencyType.CRYPTO
        } else {
            MainCurrencyType.FIAT
        }

        dispatch(AmountActionUi.SetMainCurrency(type))
    }
}