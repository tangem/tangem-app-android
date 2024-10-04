package com.tangem.core.ui.utils

import android.icu.text.CompactDecimalFormat
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.utils.StringsSigns.LOWER_SIGN
import com.tangem.utils.StringsSigns.TILDE_SIGN
import com.tangem.utils.extensions.isNotWhitespace
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@Suppress("LargeClass")
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

    @Deprecated(
        "Use formatCryptoAmount2",
        replaceWith = ReplaceWith("formatCryptoAmount2"),
    )
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

    // Migrate to this method from formatCryptoAmount ([REDACTED_TASK_KEY])
    fun formatCryptoAmount2(
        cryptoAmount: BigDecimal?,
        cryptoCurrency: String,
        decimals: Int,
        locale: Locale = Locale.getDefault(),
    ): String {
        if (cryptoAmount == null) return EMPTY_BALANCE_SIGN

        val formatter = NumberFormat.getCurrencyInstance(locale).apply {
            currency = usdCurrency
            maximumFractionDigits = decimals.coerceAtMost(maximumValue = 8)
            minimumFractionDigits = 2
            isGroupingUsed = true
            roundingMode = RoundingMode.HALF_UP
        }

        return formatter.format(cryptoAmount)
            .replaceFiatSymbolWithCrypto(
                fiatCurrencySymbol = usdCurrency.symbol,
                cryptoCurrencySymbol = cryptoCurrency,
            )
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
                    usdCurrency
                } else {
                    throw e
                }
            }
    }

    /**
     * Adds a proper currency sign for the provided formatted [amount]
     * ex. '10.0k" -> "$10.0k", "string" -> "$string"
     */
    private fun addCurrencySymbolToStringAmount(
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
     * Adds a proper currency sign for the provided formatted [amount]
     * ex. '10.0k" -> "ETH 10.0k", "string" -> "ETH string"
     */
    private fun addCryptoCurrencySymbolToStringAmount(
        amount: String,
        cryptoCurrencySymbol: String,
        locale: Locale = Locale.getDefault(),
    ): String {
        val sampleAmount = BigDecimal.TEN

        val formatter = NumberFormat.getCurrencyInstance(locale).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
            currency = usdCurrency
        }

        val formatted = formatter.format(sampleAmount)
            .replace(sampleAmount.toString(), amount)

        return formatted.replaceFiatSymbolWithCrypto(
            fiatCurrencySymbol = usdCurrency.symbol,
            cryptoCurrencySymbol = cryptoCurrencySymbol,
        )
    }

    /**
     * "123456.6" -> "$123.457K"
     * "12345.6" -> "$123.046K"
     * Negative amount is not supported
     * @param threeDigitsMethod if true, will format the amount always with 3 significant digits
     * @param scale the number of digits to the right of the decimal point
     */
    @Suppress("MagicNumber")
    fun formatCompactFiatAmount(
        amount: BigDecimal?,
        fiatCurrencyCode: String,
        fiatCurrencySymbol: String,
        threeDigitsMethod: Boolean = false,
        scale: Int = 0,
        locale: Locale = Locale.getDefault(),
    ): String {
        if (amount == null) return EMPTY_BALANCE_SIGN

        if (amount < BigDecimal.ONE) {
            return formatFiatPriceUncapped(
                fiatAmount = amount,
                fiatCurrencyCode = fiatCurrencyCode,
                fiatCurrencySymbol = fiatCurrencySymbol,
                locale = locale,
            )
        }

        val rawAmount = formatCompactAmount(
            amount = amount,
            locale = locale,
            threeDigitsMethod = threeDigitsMethod,
            scale = scale,
        )

        return addCurrencySymbolToStringAmount(
            amount = rawAmount,
            fiatCurrencyCode = fiatCurrencyCode,
            fiatCurrencySymbol = fiatCurrencySymbol,
            locale = locale,
        )
    }

    /**
     * "123456.6" -> "ETH 123.457K"
     * "12345.6" -> "123.046K ETH"
     * Negative amount is not supported
     * @param threeDigitsMethod if true, will format the amount always with 3 significant digits
     * @param scale the number of digits to the right of the decimal point
     */
    fun formatCompactCryptoAmount(
        amount: BigDecimal?,
        cryptoCurrencySymbol: String,
        threeDigitsMethod: Boolean = false,
        decimals: Int = 0,
        locale: Locale = Locale.getDefault(),
    ): String {
        if (amount == null) return EMPTY_BALANCE_SIGN

        if (amount < BigDecimal.ONE) {
            return formatCryptoAmount2(
                cryptoAmount = amount,
                cryptoCurrency = cryptoCurrencySymbol,
                decimals = decimals,
                locale = locale,
            )
        }

        val rawAmount = formatCompactAmount(
            amount = amount,
            locale = locale,
            threeDigitsMethod = threeDigitsMethod,
            scale = decimals,
        )

        return addCryptoCurrencySymbolToStringAmount(
            amount = rawAmount,
            cryptoCurrencySymbol = cryptoCurrencySymbol,
            locale = locale,
        )
    }

    /**
     * "123456.6" -> "123.457K"
     * "12345.6" -> "123.046K"
     * Negative amount is not supported
     * @param threeDigitsMethod if true, will format the amount always with 3 significant digits
     * @param scale the number of digits to the right of the decimal point
     */
    @Suppress("MagicNumber")
    fun formatCompactAmount(
        amount: BigDecimal,
        locale: Locale = Locale.getDefault(),
        threeDigitsMethod: Boolean = false,
        scale: Int = 0,
    ): String {
        if (threeDigitsMethod) {
            val scaledAmount = amount.setScale(scale, RoundingMode.HALF_UP)
            val digitsCount = scaledAmount.toString().count()
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

            return formatter.format(amount.setScale(scale, RoundingMode.HALF_UP))
        } else {
            val scaledAmount = amount.setScale(scale, RoundingMode.HALF_UP)
            val digitsCount = scaledAmount.toString().count()
            val digitsToFormat = 5 - when (digitsCount % 3) {
                0 -> 0
                1 -> 2
                else -> 1
            }

            val formatter = CompactDecimalFormat.getInstance(
                locale,
                CompactDecimalFormat.CompactStyle.SHORT,
            ).apply {
                minimumSignificantDigits = 2
                maximumSignificantDigits = digitsToFormat
            }

            return formatter.format(amount.setScale(scale, RoundingMode.HALF_UP))
        }
    }

    // Replaces fiat currency symbol with crypto currency symbol
    // with respect to the position of the symbol and whitespace
    private fun String.replaceFiatSymbolWithCrypto(fiatCurrencySymbol: String, cryptoCurrencySymbol: String): String {
        val str = this
        if (str.isEmpty()) return str

        return buildString {
            when {
                str.endsWith(fiatCurrencySymbol) -> {
                    val withoutSymbol = str.dropLast(fiatCurrencySymbol.length)
                    val last = withoutSymbol.lastOrNull() ?: return cryptoCurrencySymbol

                    append(withoutSymbol)

                    if (last.isNotWhitespace()) {
                        append("\u2009")
                    }

                    append(cryptoCurrencySymbol)
                }
                str.startsWith(fiatCurrencySymbol) -> {
                    append(cryptoCurrencySymbol)

                    val withoutSymbol = str.drop(fiatCurrencySymbol.length)
                    val first = withoutSymbol.firstOrNull()
                        ?: return cryptoCurrencySymbol

                    if (first.isNotWhitespace()) {
                        append("\u2009")
                    }

                    append(withoutSymbol)
                }
                else -> append(str)
            }
        }
    }

    private fun BigDecimal.checkFiatThreshold() = this > BigDecimal.ZERO && this < FIAT_FORMAT_THRESHOLD

    private fun BigDecimal.checkCryptoThreshold() = this > BigDecimal.ZERO && this < CRYPTO_FEE_FORMAT_THRESHOLD
}