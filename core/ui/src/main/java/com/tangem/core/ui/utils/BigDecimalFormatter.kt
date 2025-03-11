package com.tangem.core.ui.utils

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

    private val FIAT_FORMAT_THRESHOLD = BigDecimal("0.01")

    private const val FIAT_MARKET_DEFAULT_DIGITS = 2
    private const val FIAT_MARKET_EXTENDED_DIGITS = 6
    private const val FRACTIONAL_PART_LENGTH_AFTER_LEADING_ZEROES = 4

    private val usdCurrency = Currency.getInstance("USD")

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
}