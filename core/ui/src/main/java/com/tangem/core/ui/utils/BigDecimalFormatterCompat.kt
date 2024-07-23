package com.tangem.core.ui.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

internal object BigDecimalFormatterCompat {

    /**
     * Formats value as [BigDecimalFormatter.formatCompactAmount] does using only "T","B","M","K" suffixes
     * Used for < API24 compatibility
     */
    @Suppress("MagicNumber", "UnnecessaryParentheses")
    fun formatCompactAmountNoLocaleContext(
        amount: BigDecimal,
        fiatCurrencyCode: String,
        fiatCurrencySymbol: String,
        locale: Locale = Locale.getDefault(),
    ): String {
        val value = amount.setScale(0, RoundingMode.HALF_UP).longValueExact()

        val formatted = when {
            value > 1_000_000_000_000L -> {
                val trillion = value / 1_000_000_000_000
                val billion = (value % 1_000_000_000_000) / 1_000_000_000
                "$trillion.${billion}T"
            }
            value > 1_000_000_000L -> {
                val billion = value / 1_000_000_000
                val million = (value % 1_000_000_000) / 1_000_000
                "$billion.${million}B"
            }
            value > 1_000_000L -> {
                val million = value / 1_000_000
                val thousand = (value % 1_000_000) / 1_000
                "$million.${thousand}M"
            }
            value > 1_000L -> {
                val thousand = value / 1_000
                "${thousand}K"
            }
            else -> return value.toString()
        }

        return BigDecimalFormatter.addCurrencySymbolToStringAmount(
            amount = formatted,
            fiatCurrencyCode = fiatCurrencyCode,
            fiatCurrencySymbol = fiatCurrencySymbol,
            locale = locale,
        )
    }
}
