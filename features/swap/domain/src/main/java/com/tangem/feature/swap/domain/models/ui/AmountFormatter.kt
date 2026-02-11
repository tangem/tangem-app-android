package com.tangem.feature.swap.domain.models.ui

import com.tangem.feature.swap.domain.models.SwapAmount
import java.math.BigDecimal

interface AmountFormatter {

    /**
     * Use to convert crypto amount [SwapAmount] to UI representation
     *
     * @param swapAmount [SwapAmount]
     * @param currency currency symbol
     * @return formatted [String]
     */
    fun formatSwapAmountToUI(swapAmount: SwapAmount, currency: String): String

    /**
     * Use to convert ONLY crypto amount [BigDecimal] to UI representation
     *
     * @param amount
     * @param decimals
     * @param currency
     * @return formatted [String]
     */
    fun formatBigDecimalAmountToUI(amount: BigDecimal, decimals: Int, currency: String? = null): String
}