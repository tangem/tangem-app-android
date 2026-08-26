package com.tangem.data.polymarket.flow

import com.tangem.domain.polymarket.PolymarketCollateralCurrencyFactory

/**
 * The currency the prediction account's collateral is priced with.
 *
 * The collateral is not the app's USDC on Polygon — it is a different contract, held by the exchange — but it is
 * pegged to it, so the app's USDC quote is the rate to convert it into the selected fiat currency. The identifier
 * is the network-agnostic one the quote service uses, the same the payment account prices its balance with.
 */
internal val COLLATERAL_CURRENCY_ID = PolymarketCollateralCurrencyFactory.TOKEN_ID