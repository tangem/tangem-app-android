package com.tangem.core.ui.format.bigdecimal

import com.tangem.core.ui.format.bigdecimal.BigDecimalFormatConstants.CAN_BE_LOWER_SIGN
import com.tangem.core.ui.format.bigdecimal.BigDecimalFormatConstants.CRYPTO_FEE_FORMAT_THRESHOLD
import com.tangem.core.ui.format.bigdecimal.BigDecimalFormatConstants.CURRENCY_SPACE
import com.tangem.core.ui.format.bigdecimal.BigDecimalFormatConstants.FORMAT_THRESHOLD
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.utils.StringsSigns.NON_BREAKING_SPACE
import com.tangem.utils.extensions.isNotWhitespace
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

open class BigDecimalCryptoFormat(
    val symbol: String,
    val decimals: Int,
    val locale: Locale = Locale.getDefault(),
    val ignoreSymbolPosition: Boolean = false,
) : BigDecimalFormat {

    override fun invoke(value: BigDecimal): String = defaultAmount()(value)
}

class BigDecimalCryptoFormatFull(
    val cryptoCurrency: CryptoCurrency,
    locale: Locale = Locale.getDefault(),
) : BigDecimalCryptoFormat(
    symbol = cryptoCurrency.symbol,
    decimals = cryptoCurrency.decimals,
    locale = locale,
) {
    override fun invoke(value: BigDecimal): String = defaultAmount()(value)
}

// == Initializers ==

fun BigDecimalFormatScope.crypto(
    symbol: String,
    decimals: Int,
    locale: Locale = Locale.getDefault(),
): BigDecimalCryptoFormat {
    return BigDecimalCryptoFormat(
        symbol = symbol,
        decimals = decimals,
        locale = locale,
    )
}

fun BigDecimalFormatScope.crypto(
    cryptoCurrency: CryptoCurrency,
    ignoreSymbolPosition: Boolean = false,
    locale: Locale = Locale.getDefault(),
): BigDecimalCryptoFormat {
    return BigDecimalCryptoFormat(
        symbol = cryptoCurrency.symbol,
        decimals = cryptoCurrency.decimals,
        ignoreSymbolPosition = ignoreSymbolPosition,
        locale = locale,
    )
}

// == Formatters ==

fun BigDecimalCryptoFormat.defaultAmount() = BigDecimalFormat { value ->
    if (ignoreSymbolPosition) {
        val formatter = NumberFormat.getInstance(locale).apply {
            maximumFractionDigits = decimals.coerceAtMost(maximumValue = 8)
            minimumFractionDigits = 2
            isGroupingUsed = true
            roundingMode = RoundingMode.HALF_UP
        }
        formatter.format(value) + NON_BREAKING_SPACE + symbol
    } else {
        val formatter = NumberFormat.getCurrencyInstance(locale).apply {
            currency = usdCurrency
            maximumFractionDigits = decimals.coerceAtMost(maximumValue = 8)
            minimumFractionDigits = 2
            isGroupingUsed = true
            roundingMode = RoundingMode.HALF_UP
        }
        formatter.format(value)
            .replaceFiatSymbolWithCrypto(
                fiatCurrencySymbol = usdCurrency.getSymbol(locale),
                cryptoCurrencySymbol = symbol,
            )
    }
}

fun BigDecimalCryptoFormat.shorted() = BigDecimalFormat { value ->
    val formatter = if (value.isMoreThanThreshold()) {
        NumberFormat.getCurrencyInstance(locale).apply {
            currency = usdCurrency
            maximumFractionDigits = 2
            minimumFractionDigits = 2
            isGroupingUsed = true
            roundingMode = RoundingMode.HALF_UP
        }
    } else {
        NumberFormat.getCurrencyInstance(locale).apply {
            currency = usdCurrency
            maximumFractionDigits = decimals.coerceAtMost(maximumValue = 6)
            minimumFractionDigits = 2
            isGroupingUsed = true
            roundingMode = RoundingMode.DOWN
        }
    }

    formatter.format(value)
        .replaceFiatSymbolWithCrypto(
            fiatCurrencySymbol = usdCurrency.getSymbol(locale),
            cryptoCurrencySymbol = symbol,
        )
}

/**
 * Format for displaying crypto amounts with their original decimals.
 */
fun BigDecimalCryptoFormat.uncapped() = BigDecimalFormat { value ->
    val formatter = NumberFormat.getCurrencyInstance(locale).apply {
        currency = usdCurrency
        maximumFractionDigits = decimals
        minimumFractionDigits = 2
        isGroupingUsed = true
        roundingMode = RoundingMode.HALF_UP
    }

    formatter.format(value)
        .replaceFiatSymbolWithCrypto(
            fiatCurrencySymbol = usdCurrency.getSymbol(locale),
            cryptoCurrencySymbol = symbol,
        )
}

/**
 * Format for displaying crypto amounts with a fixed number of decimals.
 */
fun BigDecimalCryptoFormat.anyDecimals(maxDecimals: Int = decimals, minDecimals: Int = decimals) =
    BigDecimalFormat { value ->
        val formatter = NumberFormat.getCurrencyInstance(locale).apply {
            currency = usdCurrency
            maximumFractionDigits = maxDecimals
            minimumFractionDigits = minDecimals
            isGroupingUsed = true
            roundingMode = RoundingMode.HALF_UP
        }

        formatter.format(value)
            .replaceFiatSymbolWithCrypto(
                fiatCurrencySymbol = usdCurrency.getSymbol(locale),
                cryptoCurrencySymbol = symbol,
            )
    }

/**
 * Format for displaying fees.
 * If the fee is less than the threshold, it will be displayed as a fixed value "<0.000001 BTC", "<BTC 0.000001".
 */
fun BigDecimalCryptoFormat.fee(canBeLower: Boolean = false) = BigDecimalFormat { value ->
    val formatter = NumberFormat.getCurrencyInstance(locale).apply {
        currency = usdCurrency
        maximumFractionDigits = decimals.coerceAtMost(maximumValue = 6)
        minimumFractionDigits = 2
        isGroupingUsed = true
        roundingMode = RoundingMode.HALF_UP
    }

    if (value.lessThanFeeCryptoThreshold()) {
        buildString {
            append(CAN_BE_LOWER_SIGN)
            append(
                formatter
                    .format(CRYPTO_FEE_FORMAT_THRESHOLD)
                    .replaceFiatSymbolWithCrypto(
                        fiatCurrencySymbol = usdCurrency.getSymbol(locale),
                        cryptoCurrencySymbol = symbol,
                        addStartSpace = true,
                    ),
            )
        }
    } else {
        buildString {
            if (canBeLower) {
                append(CAN_BE_LOWER_SIGN)
            }
            append(
                formatter.format(value)
                    .replaceFiatSymbolWithCrypto(
                        fiatCurrencySymbol = usdCurrency.getSymbol(locale),
                        cryptoCurrencySymbol = symbol,
                        addStartSpace = canBeLower,
                    ),
            )
        }
    }
}

// == Helpers ==

private fun BigDecimal.isMoreThanThreshold() = this > FORMAT_THRESHOLD

private fun BigDecimal.lessThanFeeCryptoThreshold() = this > BigDecimal.ZERO && this < CRYPTO_FEE_FORMAT_THRESHOLD

private val usdCurrency = Currency.getInstance(Locale.US)

// Replaces fiat currency symbol with crypto currency symbol
// with respect to the position of the symbol and whitespace
internal fun String.replaceFiatSymbolWithCrypto(
    fiatCurrencySymbol: String,
    cryptoCurrencySymbol: String,
    addStartSpace: Boolean = false,
): String {
    val str = this
    if (str.isEmpty()) return str

    return buildString {
        when {
            str.endsWith(fiatCurrencySymbol) -> {
                val withoutSymbol = str.dropLast(fiatCurrencySymbol.length)

                if (cryptoCurrencySymbol.isBlank()) {
                    return withoutSymbol
                }

                val last = withoutSymbol.lastOrNull() ?: return cryptoCurrencySymbol

                append(withoutSymbol)

                if (last.isNotWhitespace()) {
                    append(CURRENCY_SPACE)
                }

                append(cryptoCurrencySymbol)
            }
            str.startsWith(fiatCurrencySymbol) -> {
                if (addStartSpace) {
                    append(CURRENCY_SPACE)
                }

                val withoutSymbol = str.drop(fiatCurrencySymbol.length)
                val first = withoutSymbol.firstOrNull()
                    ?: return cryptoCurrencySymbol

                if (cryptoCurrencySymbol.isBlank()) {
                    return withoutSymbol
                }

                append(cryptoCurrencySymbol)

                if (first.isNotWhitespace()) {
                    append(CURRENCY_SPACE)
                }

                append(withoutSymbol)
            }
            else -> append(str)
        }
    }
}