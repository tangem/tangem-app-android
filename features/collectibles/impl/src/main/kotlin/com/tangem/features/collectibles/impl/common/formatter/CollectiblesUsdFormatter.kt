package com.tangem.features.collectibles.impl.common.formatter

import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.domain.appcurrency.model.AppCurrency
import java.math.BigDecimal

/** Collectibles are priced in USD whatever the app currency is. */
internal fun BigDecimal?.formatUsd(): String = format {
    fiat(
        fiatCurrencyCode = AppCurrency.Default.code,
        fiatCurrencySymbol = AppCurrency.Default.symbol,
    ).optionalDecimals()
}