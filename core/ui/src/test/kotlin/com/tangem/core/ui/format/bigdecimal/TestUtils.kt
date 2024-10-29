package com.tangem.core.ui.format.bigdecimal

internal const val CURRENCY_SPACE_FOR_TESTS = '\u00a0'

internal fun String.addSymbolWithSpaceRight(symbol: String): String = "$this$CURRENCY_SPACE_FOR_TESTS$symbol"

internal fun String.addSymbolWithSpaceLeft(symbol: String): String = "$symbol$CURRENCY_SPACE_FOR_TESTS$this"