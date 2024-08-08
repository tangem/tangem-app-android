package com.tangem.core.ui.utils

import android.icu.text.CompactDecimalFormat
import android.os.Build
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.utils.StringsSigns.LOWER_SIGN
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object BigDecimalFormatter {

    const val EMPTY_BALANCE_SIGN = DASH_SIGN
    const val CAN_BE_LOWER_SIGN = LOWER_SIGN
    private val FORMAT_THRESHOLD = BigDecimal("0.01")

    private const val TEMP_CURRENCY_CODE = "USD"

    private val FIAT_FORMAT_THRESHOLD = BigDecimal("0.01")
    private const val FIAT_MARKET_DEFAULT_DIGITS = 2
    private const val FIAT_MARKET_EXTENDED_DIGITS = 6

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
            roundingMode = RoundingMode.HALF_UP
        }

        return formatter.format(cryptoAmount).let {
            if (cryptoCurrency.isEmpty()) {
                it
            } else {
                it + "\u2009$cryptoCurrency"
            }
        }
    }

    fun formatCryptoAmountShorted(
        cryptoAmount: BigDecimal?,
        cryptoCurrency: String,
        decimals: Int,
        locale: Locale = Locale.getDefault(),
    ): String {
        if (cryptoAmount == null) return EMPTY_BALANCE_SIGN

        val formatter = if (cryptoAmount.isMoreThanThreshold()) {
            NumberFormat.getNumberInstance(locale).apply {
                maximumFractionDigits = 2
                minimumFractionDigits = 2
                isGroupingUsed = true
                roundingMode = RoundingMode.HALF_UP
            }
        } else {
            NumberFormat.getNumberInstance(locale).apply {
                maximumFractionDigits = decimals.coerceAtMost(maximumValue = 6)
                minimumFractionDigits = 2
                isGroupingUsed = true
                roundingMode = RoundingMode.DOWN
            }
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
            roundingMode = RoundingMode.HALF_UP
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
            maximumFractionDigits = FIAT_MARKET_DEFAULT_DIGITS
            minimumFractionDigits = FIAT_MARKET_DEFAULT_DIGITS
            roundingMode = RoundingMode.HALF_UP
        }

        return if (fiatAmount.isLessThanThreshold()) {
            buildString {
                append(CAN_BE_LOWER_SIGN)
                append(
                    formatter.format(FIAT_FORMAT_THRESHOLD)
                        .replace(formatterCurrency.getSymbol(locale), fiatCurrencySymbol),
                )
            }
        } else {
            formatter.format(fiatAmount)
                .replace(formatterCurrency.getSymbol(locale), fiatCurrencySymbol)
        }
    }

    fun formatFiatAmountUncapped(
        fiatAmount: BigDecimal?,
        fiatCurrencyCode: String,
        fiatCurrencySymbol: String,
        locale: Locale = Locale.getDefault(),
    ): String {
        if (fiatAmount == null) return EMPTY_BALANCE_SIGN
        val formatterCurrency = getCurrency(fiatCurrencyCode)

        val digits = if (fiatAmount.isLessThanThreshold()) {
            FIAT_MARKET_EXTENDED_DIGITS
        } else {
            FIAT_MARKET_DEFAULT_DIGITS
        }
        val formatter = NumberFormat.getCurrencyInstance(locale).apply {
            currency = formatterCurrency
            maximumFractionDigits = digits
            minimumFractionDigits = FIAT_MARKET_DEFAULT_DIGITS
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

    private fun BigDecimal.isMoreThanThreshold() = this > FORMAT_THRESHOLD

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

    /**
     * Adds a proper currency sign for the provided formatted [amount]
     * ex. '10.0k" -> "$10.0k", "string" -> "$string"
     */
    fun addCurrencySymbolToStringAmount(
        amount: String,
        fiatCurrencyCode: String,
        fiatCurrencySymbol: String,
        locale: Locale = Locale.getDefault(),
    ): String {
        val sampleAmount = BigDecimal.TEN
        val currency = getCurrency(fiatCurrencyCode)

        val formatter = NumberFormat.getCurrencyInstance(locale).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
            this.currency = currency
        }

        val formatted = formatter.format(sampleAmount)
            .replace(currency.getSymbol(locale), fiatCurrencySymbol)
            .replace(sampleAmount.toString(), amount)

        return formatted
    }

    /**
     * "123456.6" -> "$123.457K"
     * "12345.6" -> "$123.046K"
     */
    @Suppress("MagicNumber")
    fun formatCompactAmount(
        amount: BigDecimal,
        fiatCurrencyCode: String,
        fiatCurrencySymbol: String,
        locale: Locale = Locale.getDefault(),
    ): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return BigDecimalFormatterCompat.formatCompactAmountNoLocaleContext(
                amount = amount,
                fiatCurrencyCode = fiatCurrencyCode,
                fiatCurrencySymbol = fiatCurrencySymbol,
                locale = locale,
            )
        }

        val scaledAmount = amount.setScale(0, RoundingMode.HALF_UP)
        val digitsCount = scaledAmount.longValueExact().toString().count()
        val digitsToFormat = 6 - when (digitsCount % 3) {
            0 -> 0
            1 -> 2
            else -> 1
        }

        val formatter = CompactDecimalFormat.getInstance(
            locale,
            CompactDecimalFormat.CompactStyle.SHORT,
        ).apply {
            minimumSignificantDigits = 4
            maximumSignificantDigits = digitsToFormat
        }

        val rawAmount = formatter.format(amount.setScale(0, RoundingMode.HALF_UP))

        return addCurrencySymbolToStringAmount(
            amount = rawAmount,
            fiatCurrencyCode = fiatCurrencyCode,
            fiatCurrencySymbol = fiatCurrencySymbol,
            locale = locale,
        )
    }

    private fun BigDecimal.isLessThanThreshold() = this > BigDecimal.ZERO && this < FIAT_FORMAT_THRESHOLD
}
