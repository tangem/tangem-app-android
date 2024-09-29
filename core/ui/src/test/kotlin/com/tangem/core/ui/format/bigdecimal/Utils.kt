package com.tangem.core.ui.format.bigdecimal

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

internal val CURRENCY_SPACE_FOR_TESTS by lazy {
    val formatter = NumberFormat.getCurrencyInstance(Locale.GERMAN).apply {
        currency = Currency.getInstance("USD")
    }
    formatter.format(BigDecimal.TEN).firstOrNull { it.isWhitespace() } ?: '\u00a0'
}

internal fun String.addSymbolWithSpaceRight(symbol: String): String = "$this$CURRENCY_SPACE_FOR_TESTS$symbol"

internal fun String.addSymbolWithSpaceLeft(symbol: String): String = "$symbol$CURRENCY_SPACE_FOR_TESTS$this"
