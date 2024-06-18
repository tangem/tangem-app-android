package com.tangem.common.ui.amountScreen.utils

import com.tangem.blockchain.common.Amount
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.utils.BigDecimalFormatter.EMPTY_BALANCE_SIGN
import com.tangem.domain.appcurrency.model.AppCurrency
import java.math.BigDecimal

private const val CRYPTO_FEE_DECIMALS = 6

fun getCryptoReference(amount: Amount?, isFeeApproximate: Boolean): TextReference? {
    if (amount == null) return null
    return combinedReference(
        if (isFeeApproximate) stringReference("${BigDecimalFormatter.CAN_BE_LOWER_SIGN} ") else TextReference.EMPTY,
        stringReference(
            BigDecimalFormatter.formatCryptoAmount(
                cryptoAmount = amount.value,
                cryptoCurrency = amount.currencySymbol,
                decimals = amount.decimals.coerceAtMost(CRYPTO_FEE_DECIMALS),
            ),
        ),
    )
}

fun getFiatReference(value: BigDecimal?, rate: BigDecimal?, appCurrency: AppCurrency): TextReference? {
    if (value == null || rate == null) return null
    val formattedFiat = getFiatString(value = value, rate = rate, appCurrency = appCurrency)
    return stringReference(formattedFiat)
}

fun getFiatString(value: BigDecimal?, rate: BigDecimal?, appCurrency: AppCurrency): String {
    if (value == null || rate == null) return EMPTY_BALANCE_SIGN
    val feeValue = value.multiply(rate)
    return BigDecimalFormatter.formatFiatAmount(
        fiatAmount = feeValue,
        fiatCurrencyCode = appCurrency.code,
        fiatCurrencySymbol = appCurrency.symbol,
    )
}
