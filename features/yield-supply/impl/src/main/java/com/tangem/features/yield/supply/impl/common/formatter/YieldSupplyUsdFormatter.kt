package com.tangem.features.yield.supply.impl.common.formatter

import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import java.math.BigDecimal

/**
 * Formats a value the incentives backend reports in USD.
 *
 * Reward estimates arrive in USD whatever currency the app displays, so the app's symbol must not be put on them.
 */
internal fun BigDecimal.formatUsd(): String = format {
    fiat(fiatCurrencyCode = AppCurrency.Default.code, fiatCurrencySymbol = AppCurrency.Default.symbol)
}