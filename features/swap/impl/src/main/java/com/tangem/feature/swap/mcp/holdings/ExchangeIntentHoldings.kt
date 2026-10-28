package com.tangem.feature.swap.mcp.holdings

import com.tangem.domain.models.currency.CryptoCurrency

/** What a wallet holds of the two assets a swap intent names. */
internal sealed interface ExchangeIntentHoldings {

    data class Both(val from: CryptoCurrency, val to: CryptoCurrency) : ExchangeIntentHoldings

    /** The wallet holds [to] but not the asset to swap from. */
    data class MissingFrom(val to: CryptoCurrency) : ExchangeIntentHoldings

    /** The wallet holds [from] but not the asset to swap to. */
    data class MissingTo(val from: CryptoCurrency) : ExchangeIntentHoldings

    data object MissingBoth : ExchangeIntentHoldings
}