package com.tangem.core.ui.format.bigdecimal

import com.tangem.core.ui.format.bigdecimal.BigDecimalFormatConstants.CAN_BE_LOWER_SIGN
import com.tangem.core.ui.format.bigdecimal.BigDecimalFormatConstants.CRYPTO_FEE_FORMAT_THRESHOLD
import com.tangem.core.ui.format.bigdecimal.BigDecimalFormatConstants.FORMAT_THRESHOLD
import com.tangem.domain.tokens.model.CryptoCurrency
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
) : BigDecimalFormat {

    override fun invoke(value: BigDecimal): String = asDefaultAmount()(value)
}

class BigDecimalCryptoFormatFull(
    val cryptoCurrency: CryptoCurrency,
    locale: Locale = Locale.getDefault(),
) : BigDecimalCryptoFormat(
    symbol = cryptoCurrency.symbol,
    decimals = cryptoCurrency.decimals,
    locale = locale,
) {
    override fun invoke(value: BigDecimal): String = asDefaultAmount()(value)
}

// == Initializers ==

fun BigDecimalFormatScope.crypto(
    cryptoCurrency: String,
    decimals: Int,
    locale: Locale = Locale.getDefault(),
): BigDecimalCryptoFormat {
    return BigDecimalCryptoFormat(
        symbol = cryptoCurrency,
        decimals = decimals,
        locale = locale,
    )
}

fun BigDecimalFormatScope.crypto(
    cryptoCurrency: CryptoCurrency,
    locale: Locale = Locale.getDefault(),
): BigDecimalCryptoFormat {
    return BigDecimalCryptoFormat(
        symbol = cryptoCurrency.symbol,
        decimals = cryptoCurrency.decimals,
        locale = locale,
    )
}

// == Formatters ==

fun BigDecimalCryptoFormat.asDefaultAmount() = BigDecimalFormat { value ->
    val formatter = NumberFormat.getCurrencyInstance(locale).apply {
        currency = usdCurrency
        maximumFractionDigits = decimals.coerceAtMost(maximumValue = 8)
        minimumFractionDigits = 2
        isGroupingUsed = true
        roundingMode = RoundingMode.HALF_UP
    }

    formatter.format(value)
        .replaceFiatSymbolWithCrypto(
            fiatCurrencySymbol = usdCurrency.symbol,
            cryptoCurrencySymbol = symbol,
        )
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
        NumberFormat.getNumberInstance(locale).apply {
            currency = usdCurrency
            maximumFractionDigits = decimals.coerceAtMost(maximumValue = 6)
            minimumFractionDigits = 2
            isGroupingUsed = true
            roundingMode = RoundingMode.DOWN
        }
    }

    formatter.format(value)
        .replaceFiatSymbolWithCrypto(
            fiatCurrencySymbol = usdCurrency.symbol,
            cryptoCurrencySymbol = symbol,
        )
}

fun BigDecimalCryptoFormatFull.uncapped() = BigDecimalFormat { value ->
    val formatter = NumberFormat.getCurrencyInstance(locale).apply {
        currency = usdCurrency
        maximumFractionDigits = cryptoCurrency.decimals
        minimumFractionDigits = 2
        isGroupingUsed = true
        roundingMode = RoundingMode.HALF_UP
    }

    formatter.format(value)
        .replaceFiatSymbolWithCrypto(
            fiatCurrencySymbol = usdCurrency.symbol,
            cryptoCurrencySymbol = cryptoCurrency.symbol,
        )
}

fun BigDecimalCryptoFormat.fee(canBeLower: Boolean = false) = BigDecimalFormat { value ->
    val formatter = NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = decimals.coerceAtMost(maximumValue = 6)
        minimumFractionDigits = 2
        isGroupingUsed = true
        roundingMode = RoundingMode.HALF_UP
    }

    val formatted = if (value.checkCryptoThreshold()) {
        buildString {
            append(CAN_BE_LOWER_SIGN)
            append(
                formatter
                    .format(CRYPTO_FEE_FORMAT_THRESHOLD)
                    .replaceFiatSymbolWithCrypto(
                        fiatCurrencySymbol = usdCurrency.symbol,
                        cryptoCurrencySymbol = symbol,
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
                        fiatCurrencySymbol = usdCurrency.symbol,
                        cryptoCurrencySymbol = symbol,
                    ),
            )
        }
    }

    formatted.addCryptoSymbol(symbol)
}

// == Helpers ==

internal fun String.addCryptoSymbol(symbol: String): String {
    return if (this.isEmpty()) {
        this
    } else {
        "$this\u2009$symbol"
    }
}

private fun BigDecimal.isMoreThanThreshold() = this > FORMAT_THRESHOLD

private fun BigDecimal.checkCryptoThreshold() = this > BigDecimal.ZERO && this < CRYPTO_FEE_FORMAT_THRESHOLD

private val usdCurrency = Currency.getInstance("USD")

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
