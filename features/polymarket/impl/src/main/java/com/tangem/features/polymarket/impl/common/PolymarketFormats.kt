package com.tangem.features.polymarket.impl.common

import com.tangem.core.ui.format.bigdecimal.compact
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import java.math.BigDecimal

/**
 * Formats a traded volume as Polymarket states it: in USD, regardless of the currency selected in the app,
 * and compacted (e.g. "$1.2M"). Shared by the feed cards and the event-details sheet.
 */
internal fun BigDecimal.formatPolymarketVolume(): String = format {
    fiat(fiatCurrencyCode = USD_CODE, fiatCurrencySymbol = USD_SYMBOL).compact()
}

private const val USD_CODE = "USD"
private const val USD_SYMBOL = "$"