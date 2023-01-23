package com.tangem.feature.swap.domain.models.ui

import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.lib.crypto.models.ProxyAmount
import com.tangem.utils.toFormattedCurrencyString
import java.math.BigDecimal

class AmountFormatter {

    fun formatSwapAmountToUI(swapAmount: SwapAmount?, currency: String): String {
        return swapAmount?.let {
            it.value.toFormattedCurrencyString(it.decimals, currency)
        } ?: ""
    }

    fun formatProxyAmountToUI(proxyAmount: ProxyAmount?, currency: String): String {
        return proxyAmount?.let {
            it.value.toFormattedCurrencyString(it.decimals, currency)
        } ?: ""
    }

    fun formatBigDecimalAmountToUI(amount: BigDecimal, decimals: Int, currency: String): String {
        return amount.toFormattedCurrencyString(decimals, currency)
    }
}
