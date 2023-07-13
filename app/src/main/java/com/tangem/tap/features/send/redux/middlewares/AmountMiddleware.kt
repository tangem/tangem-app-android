package com.tangem.tap.features.send.redux.middlewares

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionError
import com.tangem.common.extensions.isZero
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.send.redux.AmountAction
import com.tangem.tap.features.send.redux.AmountActionUi
import com.tangem.tap.features.send.redux.FeeAction
import com.tangem.tap.features.send.redux.FeeActionUi
import com.tangem.tap.features.send.redux.ReceiptAction
import com.tangem.tap.features.send.redux.SendAction
import com.tangem.tap.features.send.redux.states.MainCurrencyType
import com.tangem.tap.features.send.redux.states.SendState
import org.rekotlin.Action
import java.math.BigDecimal
import java.util.*

/**
* [REDACTED_AUTHOR]
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
        val transactionErrors = walletManager.validateTransaction(amountToSend, sendState.feeState.currentFee?.amount)
        val amountFieldErrors = filterErrorsForAmountField(transactionErrors)
        if (amountFieldErrors.isEmpty()) {
            dispatch(AmountAction.SetAmountError(null))
        } else {
            dispatch(AmountAction.SetAmountError(createValidateTransactionError(amountFieldErrors, walletManager)))
        }
        dispatch(ReceiptAction.RefreshReceipt)
        dispatch(SendAction.ChangeSendButtonState(sendState.getButtonState()))
    }

    private fun setMaxAmount(appState: AppState?, dispatch: (Action) -> Unit) {
        val sendState = appState?.sendState ?: return

        dispatch(AmountAction.SetAmount(sendState.amountState.balanceCrypto, false))
        if (sendState.amountState.canIncludeFee()) {
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

private fun filterErrorsForAmountField(errors: EnumSet<TransactionError>): EnumSet<TransactionError> {
    val showIntoAmountField = EnumSet.noneOf(TransactionError::class.java)
    errors.forEach {
        when (it) {
            TransactionError.AmountExceedsBalance -> {
                showIntoAmountField.remove(TransactionError.TotalExceedsBalance)
                showIntoAmountField.add(it)
            }
            TransactionError.FeeExceedsBalance -> {
                showIntoAmountField.remove(TransactionError.TotalExceedsBalance)
                showIntoAmountField.add(it)
            }
            TransactionError.TotalExceedsBalance -> {
                val notAcceptable = listOf(TransactionError.AmountExceedsBalance, TransactionError.FeeExceedsBalance)
                if (!showIntoAmountField.containsAll(notAcceptable)) showIntoAmountField.add(it)
                showIntoAmountField.remove(TransactionError.AmountLowerExistentialDeposit)
            }
            else -> showIntoAmountField.add(it)
        }
    }
    showIntoAmountField.remove(TransactionError.TezosSendAll)

    return showIntoAmountField
}
