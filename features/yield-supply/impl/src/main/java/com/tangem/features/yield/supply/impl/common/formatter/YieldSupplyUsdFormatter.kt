package com.tangem.features.yield.supply.impl.common.formatter

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import java.math.BigDecimal

/** The incentives backend reports values in USD regardless of the app currency. */
internal fun BigDecimal.formatUsd(): TextReference = stringReference(
    format { fiat(fiatCurrencyCode = AppCurrency.Default.code, fiatCurrencySymbol = AppCurrency.Default.symbol) },
)