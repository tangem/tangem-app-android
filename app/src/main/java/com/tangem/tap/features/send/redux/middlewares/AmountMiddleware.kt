package com.tangem.tap.features.send.redux.middlewares

import com.tangem.tap.common.CurrencyConverter
import com.tangem.tap.common.extensions.isGreaterThan
import com.tangem.tap.common.extensions.isGreaterThanOrEqual
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.send.redux.AmountAction
import com.tangem.tap.features.send.redux.ReceiptAction
import com.tangem.tap.features.send.redux.states.MainCurrencyType
import com.tangem.tap.store
import org.rekotlin.Action
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
class AmountMiddleware {

    fun handle(rawData: String?, appState: AppState?, dispatch: (Action) -> Unit) {
        val sendState = appState?.sendState ?: return

        val rawData = rawData ?: store.state.sendState.amountState.viewAmountValue
        val data = if (rawData == ".") "0.0" else rawData
        val proposedAmountValue = when {
            data.isEmpty() || data == "0" -> BigDecimal.ZERO
            else -> BigDecimal(data)
        }

        val balanceCrypto = sendState.amount?.value ?: BigDecimal.ZERO
        val amountChecker = when (sendState.amountState.mainCurrency.value) {
            MainCurrencyType.FIAT -> FiatAmountChecker(balanceCrypto, sendState.currencyConverter)
            MainCurrencyType.CRYPTO -> CryptoAmountChecker(balanceCrypto)
        }

        val checkResult = amountChecker.check(
                proposedAmountValue,
                sendState.feeState.getCurrentFee(),
                sendState.feeState.feeIsIncluded
        )
        if (checkResult.error == null) {
            dispatch(AmountAction.AmountVerification.SetAmount(checkResult.amount))
        } else {
            dispatch(AmountAction.AmountVerification.SetError(checkResult.amount, checkResult.error))
        }
        dispatch(ReceiptAction.RefreshReceipt)
    }

}

interface AmountChecker {
    data class Result(val amount: BigDecimal, val error: AmountAction.Error? = null)

    fun check(value: BigDecimal, feeCrypto: BigDecimal, feeIsIncluded: Boolean): Result
}

abstract class BaseAmountChecker(
        protected val balanceCrypto: BigDecimal
) : AmountChecker {

    override fun check(value: BigDecimal, feeCrypto: BigDecimal, feeIsIncluded: Boolean): AmountChecker.Result {
        val convertedFee = convert(feeCrypto)
        val convertedBalance = convert(balanceCrypto)
        if (feeIsIncluded) {
            return if (value.isGreaterThan(convertedFee)) {
                if (convertedBalance.isGreaterThanOrEqual(value)) {
                    AmountChecker.Result(value)
                } else {
                    AmountChecker.Result(value, AmountAction.Error.FEE_GREATER_THAN_AMOUNT)
                }
            } else {
                AmountChecker.Result(value, AmountAction.Error.AMOUNT_WITH_FEE_GREATER_THAN_BALANCE)
            }
        } else {
            val amountWithFee = value.plus(convertedFee)
            return if (convertedBalance.isGreaterThanOrEqual(amountWithFee)) {
                AmountChecker.Result(value)
            } else {
                AmountChecker.Result(value, AmountAction.Error.AMOUNT_WITH_FEE_GREATER_THAN_BALANCE)
            }
        }
    }

    protected abstract fun convert(value: BigDecimal): BigDecimal
}

class CryptoAmountChecker(
        balanceCrypto: BigDecimal
) : BaseAmountChecker(balanceCrypto) {
    override fun convert(value: BigDecimal): BigDecimal = value
}

class FiatAmountChecker(
        balanceCrypto: BigDecimal,
        private val converter: CurrencyConverter
) : BaseAmountChecker(balanceCrypto) {
    override fun convert(value: BigDecimal): BigDecimal = converter.toFiat(value)
}