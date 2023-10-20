package com.tangem.core.ui.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object BigDecimalFormatter {

    const val EMPTY_BALANCE_SIGN = "—"

    private const val TEMP_CURRENCY_CODE = "USD"

    fun formatCryptoAmount(cryptoAmount: BigDecimal, cryptoCurrency: String, decimals: Int): String {
        val formatter = NumberFormat.getNumberInstance().apply {
            maximumFractionDigits = decimals.coerceAtMost(maximumValue = 8)
            minimumFractionDigits = 2
            roundingMode = RoundingMode.DOWN
        }

        return formatter.format(cryptoAmount) + "\u2009$cryptoCurrency"
    }

    fun formatFiatAmount(
        fiatAmount: BigDecimal,
        fiatCurrencyCode: String,
        fiatCurrencySymbol: String,
        locale: Locale = Locale.getDefault(),
    ): String {
        val formatterCurrency = getCurrency(fiatCurrencyCode)
        val formatter = NumberFormat.getCurrencyInstance(locale).apply {
            currency = formatterCurrency
            maximumFractionDigits = 2
            minimumFractionDigits = 2
            roundingMode = RoundingMode.HALF_UP
        }

        return formatter.format(fiatAmount)
            .replace(formatterCurrency.getSymbol(locale), fiatCurrencySymbol)
    }

    fun formatPercent(
        percent: BigDecimal,
        useAbsoluteValue: Boolean,
        locale: Locale = Locale.getDefault(),
        maxFractionDigits: Int = 2,
        minFractionDigits: Int = 2,
    ): String {
        val formatter = NumberFormat.getPercentInstance(locale).apply {
            maximumFractionDigits = maxFractionDigits
            minimumFractionDigits = minFractionDigits
            roundingMode = RoundingMode.HALF_UP
        }
        val value = if (useAbsoluteValue) percent.abs() else percent

        return formatter.format(value)
    }

    private fun getCurrency(code: String): Currency {
        return runCatching { Currency.getInstance(code) }
            .getOrElse { e ->
                // Currency code is not valid ISO 4217 code
                if (e is IllegalArgumentException) {
                    Currency.getInstance(TEMP_CURRENCY_CODE)
                } else {
                    throw e
                }
            }
    }
}
