package com.tangem.domain.collectibles.account

import com.tangem.domain.models.currency.CryptoCurrency

/**
 * Builds the currency a gacha vault holds from what the chain says about its mint.
 *
 * The token is never derived, never in the portfolio, and the wallet-bound currency factory has nothing to build it
 * from — the way the prediction collateral is built.
 */
fun interface CollectiblesVaultCurrencyFactory {

    /**
     * @param mint base58 mint address, as `getCollectiblesVaultUsdc` reads it from the program config
     */
    fun create(mint: String): CryptoCurrency.Token
}