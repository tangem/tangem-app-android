package com.tangem.tap.features.send.redux.middlewares

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionError
import com.tangem.common.extensions.isZero
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.TapError
import com.tangem.tap.features.send.redux.AmountAction
import com.tangem.tap.features.send.redux.ReceiptAction
import com.tangem.tap.features.send.redux.SendAction
import com.tangem.tap.store
import org.rekotlin.Action
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
class AmountMiddleware {

    fun handle(rawData: String?, appState: AppState?, dispatch: (Action) -> Unit) {
        val sendState = appState?.sendState ?: return
        val walletManager = sendState.walletManager ?: return

        val rawData = rawData ?: store.state.sendState.amountState.viewAmountValue
        val data = if (rawData == ".") "0.0" else rawData
        val inputValue = when {
            data.isEmpty() || data == "0" -> BigDecimal.ZERO
            else -> BigDecimal(data)
        }

        if (inputValue.isZero() && sendState.amountState.amountToSendCrypto.isZero()) return

        val inputCrypto = sendState.convertInputValueToCrypto(inputValue)
        val feeCrypto = sendState.feeState.getCurrentFee()

        val feeAmount = Amount(feeCrypto, walletManager.wallet.blockchain)
        val totalAmount = Amount(sendState.getTotalAmountToSend(inputCrypto), walletManager.wallet.blockchain)

        val transactionErrors = walletManager.validateTransaction(totalAmount, feeAmount)
        if (transactionErrors.isEmpty()) {
            dispatch(AmountAction.AmountVerification.SetAmount(inputValue))
        } else {
            val tapErrors = transactionErrors.map {
                when (it) {
                    TransactionError.AmountExceedsBalance -> TapError.AmountExceedsBalance
                    TransactionError.FeeExceedsBalance -> TapError.FeeExceedsBalance
                    TransactionError.TotalExceedsBalance -> TapError.TotalExceedsBalance
                    TransactionError.InvalidAmountValue -> TapError.InvalidAmountValue
                    TransactionError.InvalidFeeValue -> TapError.InvalidFeeValue
                    TransactionError.DustAmount -> TapError.DustAmount
                    TransactionError.DustChange -> TapError.DustChang
                    else -> TapError.UnknownError
                }
            }
            val error = TapError.ValidateTransactionErrors(tapErrors) { it.joinToString("\r\n") }
            dispatch(AmountAction.AmountVerification.SetError(inputValue, error))
        }
        dispatch(ReceiptAction.RefreshReceipt)
        dispatch(SendAction.ChangeSendButtonState(sendState.getButtonState()))
    }

}