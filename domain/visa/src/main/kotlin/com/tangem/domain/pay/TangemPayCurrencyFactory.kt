package com.tangem.domain.pay

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Factory that builds the [CryptoCurrency.Token] used by Tangem Pay (USDC on Polygon) for a given user wallet.
 *
 * Replaces the deprecated `TangemPayCryptoCurrencyFactory`: callers no longer pass the chain id explicitly —
 * the underlying network is resolved from the wallet.
 */
interface TangemPayCurrencyFactory {

    /**
     * Builds the Tangem Pay token bound to the network of the wallet identified by [userWalletId].
     *
     * @throws IllegalStateException if no wallet with [userWalletId] is currently loaded.
     */
    fun create(userWalletId: UserWalletId): CryptoCurrency.Token

    /** Hardcoded token metadata for the Tangem Pay currency (USDC on Polygon). */
    companion object {
        /** CoinGecko-style raw id used to query quotes for the Tangem Pay token. */
        val TOKEN_ID = CryptoCurrency.RawID("usd-coin")
        const val TOKEN_NAME = "USDC"
        const val TOKEN_CONTRACT_ADDRESS = "0x3c499c542cef5e3811e1192ce70d8cc03d5c3359"
        const val TOKEN_DECIMALS = 6
    }
}