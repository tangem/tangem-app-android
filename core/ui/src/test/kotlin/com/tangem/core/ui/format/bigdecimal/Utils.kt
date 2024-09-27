package com.tangem.core.ui.format.bigdecimal

internal fun String.addSymbolWithSpaceRight(symbol: String): String = "$this\u00a0$symbol"

internal fun String.addSymbolWithSpaceLeft(symbol: String): String = "$symbol\u00a0$this"
