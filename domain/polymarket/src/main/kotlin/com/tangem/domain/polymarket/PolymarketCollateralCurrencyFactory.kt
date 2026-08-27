package com.tangem.domain.polymarket

import com.tangem.domain.models.currency.CryptoCurrency

/**
 * The collateral is a contract of the exchange's own, not a token of the wallet's portfolio, so it is described
 * here rather than looked up among the currencies a user holds.
 */
interface PolymarketCollateralCurrencyFactory {

    fun create(): CryptoCurrency.Token

    companion object {
        const val TOKEN_NAME = "USDC"
        const val TOKEN_SYMBOL = "USDC"
    }
}