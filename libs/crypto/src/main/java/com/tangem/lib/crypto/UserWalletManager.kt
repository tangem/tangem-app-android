package com.tangem.lib.crypto

import com.tangem.lib.crypto.models.Currency
import com.tangem.lib.crypto.models.ProxyAmount
import com.tangem.lib.crypto.models.ProxyFiatCurrency

/**
 * Provider for user tokens data
 */
interface UserWalletManager {

    /**
     * Returns all user tokens (merged from local and backend)
     */
    suspend fun getUserTokens(networkId: String): List<Currency>

    /**
     * Returns user walletId
     */
    fun getWalletId(): String

    /**
     * Checks that token added to user wallet
     *
     * @param currency to receive referral payments
     */
    suspend fun isTokenAdded(currency: Currency): Boolean

    /**
     * Adds token to wallet if its not
     *
     * @param currency to add to wallet
     */
    fun addToken(currency: Currency)

    /**
     * Returns wallet public address for token
     *
     * @param networkId for currency
     */
    fun getWalletAddress(networkId: String): String

    /**
     * Return balances from wallet found by networkId
     *
     * @param networkId
     * @return map of <Symbol, [ProxyAmount]>
     */
    @Throws(IllegalStateException::class)
    fun getCurrentWalletTokensBalance(networkId: String): Map<String, ProxyAmount>

    /**
     * Returns selected app currency
     */
    fun getUserAppCurrency(): ProxyFiatCurrency

    /**
     * @param networkId
     * @return currency name
     */
    fun getCurrencyByNetworkId(networkId: String): String
}
