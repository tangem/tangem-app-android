package com.tangem.feature.swap.di.impl

import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.ui.AmountFormatter
import java.math.BigDecimal

class DefaultAmountFormatter : AmountFormatter {

    override fun formatSwapAmountToUI(swapAmount: SwapAmount, currency: String): String {
        return swapAmount.value.format { crypto(symbol = currency, decimals = swapAmount.decimals) }
    }

    override fun formatBigDecimalAmountToUI(amount: BigDecimal, decimals: Int, currency: String?): String {
        return amount.format { crypto(symbol = currency.orEmpty(), decimals = decimals) }
    }
}