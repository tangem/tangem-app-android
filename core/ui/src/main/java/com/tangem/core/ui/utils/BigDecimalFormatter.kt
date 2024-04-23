package com.tangem.core.ui.utils

import com.tangem.domain.tokens.model.CryptoCurrency
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object BigDecimalFormatter {

    const val EMPTY_BALANCE_SIGN = "—"
    const val CAN_BE_LOWER_SIGN = "<"

    private const val TEMP_CURRENCY_CODE = "USD"

    fun formatCryptoAmount(
        cryptoAmount: BigDecimal?,
        cryptoCurrency: String,
        decimals: Int,
        locale: Locale = Locale.getDefault(),
    ): String {
        if (cryptoAmount == null) return EMPTY_BALANCE_SIGN

        val formatter = NumberFormat.getNumberInstance(locale).apply {
            maximumFractionDigits = decimals.coerceAtMost(maximumValue = 8)
            minimumFractionDigits = 2
            isGroupingUsed = true
            roundingMode = RoundingMode.DOWN
        }

        return formatter.format(cryptoAmount).let {
            if (cryptoCurrency.isEmpty()) {
                it
            } else {
                it + "\u2009$cryptoCurrency"
            }
        }
    }

    fun formatCryptoAmountUncapped(
        cryptoAmount: BigDecimal?,
        cryptoCurrency: CryptoCurrency,
        locale: Locale = Locale.getDefault(),
    ): String {
        if (cryptoAmount == null) return EMPTY_BALANCE_SIGN

        val formatter = NumberFormat.getNumberInstance(locale).apply {
            maximumFractionDigits = cryptoCurrency.decimals
            minimumFractionDigits = 2
            isGroupingUsed = true
            roundingMode = RoundingMode.DOWN
        }

        return formatter.format(cryptoAmount).let {
            if (cryptoCurrency.symbol.isEmpty()) {
                it
            } else {
                it + "\u2009${cryptoCurrency.symbol}"
            }
        }
    }

    fun formatCryptoAmount(
        cryptoAmount: BigDecimal?,
        cryptoCurrency: CryptoCurrency,
        locale: Locale = Locale.getDefault(),
    ): String {
        return formatCryptoAmount(cryptoAmount, cryptoCurrency.symbol, cryptoCurrency.decimals, locale)
    }

    fun formatFiatAmount(
        fiatAmount: BigDecimal?,
        fiatCurrencyCode: String,
        fiatCurrencySymbol: String,
        locale: Locale = Locale.getDefault(),
    ): String {
        if (fiatAmount == null) return EMPTY_BALANCE_SIGN

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

    fun formatFiatEditableAmount(
        fiatAmount: String?,
        fiatCurrencyCode: String,
        fiatCurrencySymbol: String,
        locale: Locale = Locale.getDefault(),
    ): String {
        if (fiatAmount == null) return EMPTY_BALANCE_SIGN

        val formatterCurrency = getCurrency(fiatCurrencyCode)
        val numberFormatter = NumberFormat.getCurrencyInstance(locale).apply {
            currency = formatterCurrency
        }
        val formatter = requireNotNull(numberFormatter as? DecimalFormat) {
            Timber.e("NumberFormat is null")
            return EMPTY_BALANCE_SIGN
        }
        return "${formatter.positivePrefix}$fiatAmount${formatter.positiveSuffix}"
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

    fun formatWithSymbol(amount: String, symbol: String) = "$amount\u2009$symbol"

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
