package com.tangem.features.polymarket.impl.common

import com.tangem.core.ui.format.bigdecimal.compact
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Formats a traded volume as Polymarket states it: in USD, regardless of the currency selected in the app,
 * and compacted (e.g. "$1.2M"). Shared by the feed cards and the event-details screen.
 */
internal fun BigDecimal.formatPolymarketVolume(): String = format {
    fiat(fiatCurrencyCode = PolymarketConst.USD_CODE, fiatCurrencySymbol = PolymarketConst.USD_SYMBOL).compact()
}

/** Formats a collateral amount the way the trade screens state it: plain USD, never the app's fiat. */
internal fun BigDecimal.formatPolymarketMoney(): String = format {
    fiat(fiatCurrencyCode = PolymarketConst.USD_CODE, fiatCurrencySymbol = PolymarketConst.USD_SYMBOL)
}

/** Formats the sum the user entered, keeping cents only when they were typed: "$4,000", but "$25.50". */
internal fun BigDecimal.formatPolymarketEnteredAmount(): String = NumberFormat
    .getCurrencyInstance(Locale.getDefault())
    .apply {
        currency = Currency.getInstance(PolymarketConst.USD_CODE)
        minimumFractionDigits = 0
        maximumFractionDigits = PolymarketConst.COLLATERAL_DECIMALS
        roundingMode = RoundingMode.HALF_UP
    }
    .format(this)

/** Formats a collateral balance as the token it is held in, e.g. "1,000.00 USDC". */
internal fun BigDecimal.formatPolymarketBalance(tokenSymbol: String): String = format {
    crypto(symbol = tokenSymbol, decimals = PolymarketConst.COLLATERAL_DECIMALS, ignoreSymbolPosition = true)
}