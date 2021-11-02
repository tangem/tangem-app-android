package com.tangem.tap.features.send.redux.middlewares

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionError
import com.tangem.common.extensions.isZero
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.send.redux.*
import com.tangem.tap.features.send.redux.states.MainCurrencyType
import com.tangem.tap.features.send.redux.states.SendState
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

        var input = amountState.toBigDecimalSeparator(data)
        input = if (input == ".") "0.0" else input
        val inputValue = when {
            input.isEmpty() || input == "0" -> BigDecimal.ZERO
            else -> BigDecimal(input)
        }
        if (inputValue.isZero() && amountState.amountToSendCrypto.isZero()) return

        val inputValueCrypto = if (amountState.mainCurrency.type == MainCurrencyType.CRYPTO) {
            inputValue
        } else {
            sendState.convertFiatToExtractCrypto(inputValue)
        }

        dispatch(AmountAction.SetAmount(inputValueCrypto, true))
        dispatch(AmountActionUi.CheckAmountToSend)
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
        transactionErrors.remove(TransactionError.TezosSendAll)
        if (transactionErrors.isEmpty()) {
            dispatch(AmountAction.SetAmountError(null))
        } else {
            val amountErrors = extractErrorsForAmountField(transactionErrors)
            if (amountErrors.isNotEmpty()) {
                transactionErrors.removeAll(amountErrors)
                dispatch(AmountAction.SetAmountError(createValidateTransactionError(amountErrors, walletManager)))
            }
            if (transactionErrors.isNotEmpty()) {
                dispatch(SendAction.SendError(createValidateTransactionError(transactionErrors, walletManager)))
            }
        }
        dispatch(ReceiptAction.RefreshReceipt)
        dispatch(SendAction.ChangeSendButtonState(sendState.getButtonState()))
    }

    private fun setMaxAmount(appState: AppState?, dispatch: (Action) -> Unit) {
        val sendState = appState?.sendState ?: return

        dispatch(AmountAction.SetAmount(sendState.amountState.balanceCrypto, false))
        if (sendState.amountState.isCoinAmount()) {
            dispatch(FeeAction.ChangeLayoutVisibility(main = true, controls = true))
            dispatch(FeeActionUi.ChangeIncludeFee(true))
        }

        dispatch(AmountActionUi.CheckAmountToSend)
        if (SendState.isReadyToRequestFee()) dispatch(FeeAction.RequestFee)
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