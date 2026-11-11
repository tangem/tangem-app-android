package com.tangem.features.collectibles.impl.common.formatter

import com.tangem.core.ui.format.bigdecimal.anyDecimals
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.collectibles.account.CollectiblesVaultCurrencies
import java.math.BigDecimal

/** Collectibles are priced in USD whatever the app currency is. */
internal fun BigDecimal?.formatUsd(): String = format {
    fiat(
        fiatCurrencyCode = AppCurrency.Default.code,
        fiatCurrencySymbol = AppCurrency.Default.symbol,
    ).optionalDecimals()
}

/** @param minDecimals null keeps the default crypto format, at least two decimals */
internal fun BigDecimal?.formatUsdc(minDecimals: Int? = null): String = format {
    val usdc = crypto(
        symbol = CollectiblesVaultCurrencies.TOKEN_SYMBOL,
        decimals = CollectiblesVaultCurrencies.DECIMALS,
        ignoreSymbolPosition = true,
    )

    if (minDecimals == null) usdc else usdc.anyDecimals(minDecimals = minDecimals)
}