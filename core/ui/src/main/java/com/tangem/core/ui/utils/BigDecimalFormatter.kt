package com.tangem.core.ui.utils

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.utils.StringsSigns.LOWER_SIGN
import com.tangem.utils.StringsSigns.TILDE_SIGN
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@Suppress("LargeClass")
@Deprecated("Use BigDecimal.format")
object BigDecimalFormatter {

    const val EMPTY_BALANCE_SIGN = DASH_SIGN
    private const val CAN_BE_LOWER_SIGN = LOWER_SIGN
    private val FORMAT_THRESHOLD = BigDecimal("0.01")

    private val FIAT_FORMAT_THRESHOLD = BigDecimal("0.01")
    private val CRYPTO_FEE_FORMAT_THRESHOLD = BigDecimal("0.000001")

    private const val FIAT_MARKET_DEFAULT_DIGITS = 2
    private const val FIAT_MARKET_EXTENDED_DIGITS = 6
    private const val FRACTIONAL_PART_LENGTH_AFTER_LEADING_ZEROES = 4

    private val usdCurrency = Currency.getInstance("USD")

    @Deprecated("Use BigDecimal.format")
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

    @Deprecated("Use BigDecimal.format")
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

    @Deprecated("Use BigDecimal.format")
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

    @Deprecated("Use BigDecimal.format")
    fun formatCryptoFeeAmount(
        cryptoAmount: BigDecimal?,
        cryptoCurrency: String,
        decimals: Int,
        canBeLower: Boolean = false,
        locale: Locale = Locale.getDefault(),
    ): String {
        if (cryptoAmount == null) return EMPTY_BALANCE_SIGN

        val formatter = NumberFormat.getNumberInstance(locale).apply {
            maximumFractionDigits = decimals.coerceAtMost(maximumValue = 6)
            minimumFractionDigits = 2
            isGroupingUsed = true
            roundingMode = RoundingMode.HALF_UP
        }

        val amountFormatted = if (cryptoAmount.checkCryptoThreshold()) {
            buildString {
                append(CAN_BE_LOWER_SIGN)
                append(
                    formatter.format(CRYPTO_FEE_FORMAT_THRESHOLD),
                )
            }
        } else {
            buildString {
                if (canBeLower) {
                    append(CAN_BE_LOWER_SIGN)
                }
                append(formatter.format(cryptoAmount))
            }
        }

        return if (cryptoCurrency.isEmpty()) {
            amountFormatted
        } else {
            amountFormatted + "\u2009$cryptoCurrency"
        }
    }

    @Deprecated("Use BigDecimal.format")
    fun formatCryptoAmount(
        cryptoAmount: BigDecimal?,
        cryptoCurrency: CryptoCurrency,
        locale: Locale = Locale.getDefault(),
    ): String {
        return formatCryptoAmount(cryptoAmount, cryptoCurrency.symbol, cryptoCurrency.decimals, locale)
    }

    @Deprecated("Use BigDecimal.format")
    fun formatFiatAmount(
        fiatAmount: BigDecimal?,
        fiatCurrencyCode: String,
        fiatCurrencySymbol: String,
        decimals: Int = FIAT_MARKET_DEFAULT_DIGITS,
        locale: Locale = Locale.getDefault(),
        withApproximateSign: Boolean = false,
    ): String {
        if (fiatAmount == null) return EMPTY_BALANCE_SIGN

        val formatterCurrency = getCurrency(fiatCurrencyCode)
        val formatter = NumberFormat.getCurrencyInstance(locale).apply {
            currency = formatterCurrency
            maximumFractionDigits = decimals
            minimumFractionDigits = decimals
            roundingMode = RoundingMode.HALF_UP
        }

        return if (fiatAmount.checkFiatThreshold()) {
            buildString {
                append(CAN_BE_LOWER_SIGN)
                append(
                    formatter.format(FIAT_FORMAT_THRESHOLD)
                        .replace(formatterCurrency.getSymbol(locale), fiatCurrencySymbol),
                )
            }
        } else {
            val formattedAmount = formatter.format(fiatAmount)
                .replace(formatterCurrency.getSymbol(locale), fiatCurrencySymbol)

            if (withApproximateSign) {
                buildString {
                    append(TILDE_SIGN)
                    append(formattedAmount)
                }
            } else {
                formattedAmount
            }
        }
    }

    @Deprecated("Use BigDecimal.format")
    fun formatFiatAmountUncapped(
        fiatAmount: BigDecimal?,
        fiatCurrencyCode: String,
        fiatCurrencySymbol: String,
        locale: Locale = Locale.getDefault(),
    ): String {
        if (fiatAmount == null) return EMPTY_BALANCE_SIGN
        val formatterCurrency = getCurrency(fiatCurrencyCode)

        val digits = if (fiatAmount.checkFiatThreshold()) {
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

    @Deprecated("Use BigDecimal.format")
    fun formatFiatPriceUncapped(
        fiatAmount: BigDecimal?,
        fiatCurrencyCode: String,
        fiatCurrencySymbol: String,
        locale: Locale = Locale.getDefault(),
    ): String {
        if (fiatAmount == null) return EMPTY_BALANCE_SIGN
        val formatterCurrency = getCurrency(fiatCurrencyCode)

        val (formattedAmount, finalScale) = getFiatPriceUncappedWithScale(value = fiatAmount)

        val formatter = NumberFormat.getCurrencyInstance(locale).apply {
            currency = formatterCurrency
            maximumFractionDigits = finalScale
            minimumFractionDigits = FIAT_MARKET_DEFAULT_DIGITS
            roundingMode = RoundingMode.HALF_UP
        }

        return formatter.format(formattedAmount)
            .replace(formatterCurrency.getSymbol(locale), fiatCurrencySymbol)
    }

    @Deprecated("Use BigDecimal.format")
    fun getFiatPriceUncappedWithScale(value: BigDecimal): Pair<BigDecimal, Int> {
        return if (value < BigDecimal.ONE) {
            val leadingZeroes = value.scale() - value.precision()
            val scale = leadingZeroes + FRACTIONAL_PART_LENGTH_AFTER_LEADING_ZEROES

            val amount = value
                .setScale(scale, RoundingMode.HALF_UP)
                .stripTrailingZeros()

            amount to amount.scale()
        } else {
            value to FIAT_MARKET_DEFAULT_DIGITS
        }
    }

    @Deprecated("Use BigDecimal.format")
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
                    usdCurrency
                } else {
                    throw e
                }
            }
    }

    private fun BigDecimal.checkFiatThreshold() = this > BigDecimal.ZERO && this < FIAT_FORMAT_THRESHOLD

    private fun BigDecimal.checkCryptoThreshold() = this > BigDecimal.ZERO && this < CRYPTO_FEE_FORMAT_THRESHOLD
}