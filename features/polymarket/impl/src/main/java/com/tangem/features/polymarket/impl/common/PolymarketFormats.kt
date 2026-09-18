package com.tangem.features.polymarket.impl.common

import com.tangem.core.ui.extensions.SpanStyleReference
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.format.bigdecimal.compact
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.formatStyled
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.format.bigdecimal.simple
import java.math.BigDecimal

/**
 * Formats a traded volume as Polymarket states it: in USD, regardless of the currency selected in the app,
 * and compacted (e.g. "$1.2M"). Shared by the feed cards and the event-details screen.
 */
internal fun BigDecimal?.formatPolymarketVolume(): String = format {
    fiat(fiatCurrencyCode = PolymarketConst.USD_CODE, fiatCurrencySymbol = PolymarketConst.USD_SYMBOL).compact()
}

/** Formats a collateral amount the way the trade screens state it: plain USD, never the app's fiat. */
internal fun BigDecimal?.formatPolymarketMoney(): String = format {
    fiat(fiatCurrencyCode = PolymarketConst.USD_CODE, fiatCurrencySymbol = PolymarketConst.USD_SYMBOL)
}

/** [formatPolymarketMoney] with the cents set apart in [fractionStyle], as the balance header states them. */
internal fun BigDecimal?.formatPolymarketMoneyStyled(fractionStyle: SpanStyleReference): TextReference = formatStyled {
    fiat(
        fiatCurrencyCode = PolymarketConst.USD_CODE,
        fiatCurrencySymbol = PolymarketConst.USD_SYMBOL,
        spanStyleReference = fractionStyle,
    )
}

/** Formats the sum the user entered, keeping cents only when they were typed: "$4,000", but "$25.50". */
internal fun BigDecimal.formatPolymarketEnteredAmount(): String = format {
    fiat(fiatCurrencyCode = PolymarketConst.USD_CODE, fiatCurrencySymbol = PolymarketConst.USD_SYMBOL)
        .optionalDecimals()
}

/** Formats a collateral balance as the token it is held in, e.g. "1,000.00 USDC". */
internal fun BigDecimal?.formatPolymarketBalance(tokenSymbol: String): String = format {
    crypto(symbol = tokenSymbol, decimals = PolymarketConst.COLLATERAL_DECIMALS, ignoreSymbolPosition = true)
}

/** Formats a slippage tolerance the way the screens state it, e.g. "0.25%". */
internal fun BigDecimal.formatSlippagePercent(): String = movePointLeft(2).format {
    percent(minFractionDigits = 0)
}

/** Formats a share count the way the result screen states it: grouped, fraction only when present, e.g. "4,612.54". */
internal fun BigDecimal.formatPolymarketShares(): String = format { simple(decimals = 2) }

/** @return a whole-percent share the way the calculator buttons state it, e.g. "25%". */
internal fun Int.formatPolymarketPercent(): String = BigDecimal(this).movePointLeft(2).format {
    percent(minFractionDigits = 0)
}