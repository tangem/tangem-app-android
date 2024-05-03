package com.tangem.features.send.impl.presentation.utils

import com.tangem.blockchain.common.Amount
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.utils.BigDecimalFormatter.EMPTY_BALANCE_SIGN
import com.tangem.domain.appcurrency.model.AppCurrency
import java.math.BigDecimal
import java.math.RoundingMode

private const val FIAT_DECIMALS = 2
private const val FEE_MINIMUM_VALUE = 0.01

internal fun getCryptoReference(amount: Amount?, isFeeApproximate: Boolean): TextReference? {
    if (amount == null) return null
    return combinedReference(
        if (isFeeApproximate) stringReference("${BigDecimalFormatter.CAN_BE_LOWER_SIGN} ") else TextReference.EMPTY,
        stringReference(
            BigDecimalFormatter.formatCryptoAmount(
                cryptoAmount = amount.value,
                cryptoCurrency = amount.currencySymbol,
                decimals = amount.decimals,
            ),
        ),
    )
}

internal fun getFiatReference(value: BigDecimal?, rate: BigDecimal?, appCurrency: AppCurrency): TextReference? {
    if (value == null || rate == null) return null
    val formattedFiat = getFiatString(value = value, rate = rate, appCurrency = appCurrency)
    return stringReference(formattedFiat)
}

internal fun getFiatString(value: BigDecimal?, rate: BigDecimal?, appCurrency: AppCurrency): String {
    if (value == null || rate == null) return EMPTY_BALANCE_SIGN
    val feeValue = value.multiply(rate)
    val scaled = feeValue.setScale(FIAT_DECIMALS, RoundingMode.UP) ?: BigDecimal.ZERO
    val formattedValue = if (scaled < BigDecimal(FEE_MINIMUM_VALUE)) {
        buildString {
            append(BigDecimalFormatter.CAN_BE_LOWER_SIGN)
            append(
                BigDecimalFormatter.formatFiatAmount(
                    fiatAmount = BigDecimal(FEE_MINIMUM_VALUE),
                    fiatCurrencyCode = appCurrency.code,
                    fiatCurrencySymbol = appCurrency.symbol,
                ),
            )
        }
    } else {
        BigDecimalFormatter.formatFiatAmount(
            fiatAmount = feeValue,
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
        )
    }
    return formattedValue
}