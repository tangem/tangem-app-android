package com.tangem.features.send.impl.presentation.utils

import com.tangem.blockchain.common.Amount
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import java.math.BigDecimal

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

internal fun getFiatReference(amount: Amount?, rate: BigDecimal?, appCurrency: AppCurrency): TextReference? {
    if (amount == null) return null
    return stringReference(
        BigDecimalFormatter.formatFiatAmount(
            fiatAmount = rate?.let { amount.value?.multiply(it) },
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
        ),
    )
}