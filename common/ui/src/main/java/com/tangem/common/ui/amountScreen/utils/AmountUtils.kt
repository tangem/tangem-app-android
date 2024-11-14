package com.tangem.common.ui.amountScreen.utils

import androidx.compose.ui.text.input.ImeAction
import com.tangem.common.ui.amountScreen.models.AmountFieldModel
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.core.ui.utils.parseToBigDecimal
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
    val currencyCryptoAmount = maxEnterAmount.amount ?: BigDecimal.ZERO
    val currencyFiatAmount = maxEnterAmount.fiatAmount ?: BigDecimal.ZERO
    val fiatDecimal = parseToBigDecimal(amountTextField.fiatAmount.decimals)
    val cryptoDecimal = parseToBigDecimal(amountTextField.cryptoAmount.decimals)
    return if (amountTextField.isFiatValue) {
        fiatDecimal > currencyFiatAmount
    } else {
        cryptoDecimal > currencyCryptoAmount
    }
}

internal fun getKeyboardAction(isCheckFailed: Boolean, decimalCryptoValue: BigDecimal) =
    if (!isCheckFailed && !decimalCryptoValue.isZero()) {
        ImeAction.Done
    } else {
        ImeAction.None
    }