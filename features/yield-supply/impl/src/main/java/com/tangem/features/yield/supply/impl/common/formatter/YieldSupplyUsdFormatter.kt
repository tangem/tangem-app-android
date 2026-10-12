package com.tangem.features.yield.supply.impl.common.formatter

import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import java.math.BigDecimal

private const val USD_CODE = "USD"
private const val USD_SYMBOL = "$"

/**
 * Formats a value the incentives backend reports in USD.
 *
 * Reward estimates arrive in USD whatever currency the app displays, so the app's symbol must not be put on them.
 */
internal fun BigDecimal.formatUsd(): String = format {
    fiat(fiatCurrencyCode = USD_CODE, fiatCurrencySymbol = USD_SYMBOL)
}