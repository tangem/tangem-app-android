package com.tangem.common.ui.amountScreen.utils

import androidx.compose.ui.text.input.ImeAction
import com.tangem.common.ui.R
import com.tangem.common.ui.amountScreen.models.AmountFieldModel
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.utils.extensions.isZero
import java.math.BigDecimal
import java.math.RoundingMode

internal fun String.getCryptoValue(fiatRate: BigDecimal?, isFiatValue: Boolean, decimals: Int): String {
    return if (isFiatValue && fiatRate != null) {
        parseToBigDecimal(decimals).divide(fiatRate, decimals, RoundingMode.DOWN)
            .parseBigDecimal(decimals)
    } else {
        this
    }
}

internal fun String.getFiatValue(
    fiatRate: BigDecimal?,
    isFiatValue: Boolean,
    decimals: Int,
): Pair<String, BigDecimal?> {
    return if (fiatRate != null) {
        val fiatValue = if (!isFiatValue) {
            parseToBigDecimal(decimals).multiply(fiatRate).parseBigDecimal(decimals)
        } else {
            this
        }
        val decimalFiatValue = fiatValue.parseToBigDecimal(decimals)
        fiatValue to decimalFiatValue
    } else {
        "" to null
    }
}

internal fun String.checkExceedBalance(
    maxEnterAmount: EnterAmountBoundary,
    amountTextField: AmountFieldModel,
): Boolean {
    val fiatDecimal = parseToBigDecimal(amountTextField.fiatAmount.decimals)
    val cryptoDecimal = parseToBigDecimal(amountTextField.cryptoAmount.decimals)
    return if (amountTextField.isFiatValue) {
        val currencyFiatAmount = maxEnterAmount.fiatAmount ?: return false
        fiatDecimal > currencyFiatAmount
    } else {
        val currencyCryptoAmount = maxEnterAmount.amount ?: return false
        cryptoDecimal > currencyCryptoAmount
    }
}

internal fun getKeyboardAction(isCheckFailed: Boolean, decimalCryptoValue: BigDecimal) =
    if (!isCheckFailed && !decimalCryptoValue.isZero()) {
        ImeAction.Done
    } else {
        ImeAction.None
    }

internal fun getAmountValidationError(
    isExceedBalance: Boolean,
    isLessThanMinimum: Boolean,
    minimumTransactionAmount: EnterAmountBoundary?,
    cryptoCurrency: CryptoCurrency,
): TextReference = when {
    isExceedBalance -> resourceReference(R.string.common_insufficient_balance)
    isLessThanMinimum -> {
        val minimumAmount = minimumTransactionAmount?.amount.format { crypto(cryptoCurrency) }
        resourceReference(
            R.string.transfer_notification_invalid_minimum_transaction_amount_text,
            wrappedList(minimumAmount, minimumAmount),
        )
    }
    else -> TextReference.EMPTY
}