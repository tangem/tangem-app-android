package com.tangem.features.swap.v2.impl.common

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.SwapAmountType

internal fun resolveAmountErrorCurrency(
    fromCryptoCurrency: CryptoCurrency,
    toCryptoCurrency: CryptoCurrency,
    amountType: SwapAmountType?,
): CryptoCurrency = when (amountType) {
    SwapAmountType.To -> toCryptoCurrency
    SwapAmountType.From, null -> fromCryptoCurrency
}