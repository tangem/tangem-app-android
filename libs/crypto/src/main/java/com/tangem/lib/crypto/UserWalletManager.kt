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
    @Throws(IllegalStateException::class)
    suspend fun getUserTokens(networkId: String, isExcludeCustom: Boolean): List<Currency>

    @Throws(IllegalStateException::class)
    fun getNativeTokenForNetwork(networkId: String): Currency

    /**
     * Returns user walletId
     */
    fun getWalletId(): String

    /**
     * Checks that token added to user wallet
     *
     * @param currency to receive referral payments
     */
    @Throws(IllegalStateException::class)
    suspend fun isTokenAdded(currency: Currency): Boolean

    /**
     * Adds token to wallet if its not
     *
     * @param currency to add to wallet
     */
    @Throws(IllegalStateException::class)
    suspend fun addToken(currency: Currency)

    /**
     * Returns wallet public address for token
     *
     * @param networkId for currency
     */
    @Throws(IllegalStateException::class)
    fun getWalletAddress(networkId: String): String

    /**
     * Return balances from wallet found by networkId
     *
     * @param networkId
     * @return map of <Symbol, [ProxyAmount]>
     */
    @Throws(IllegalStateException::class)
    suspend fun getCurrentWalletTokensBalance(networkId: String, extraTokens: List<Currency>): Map<String, ProxyAmount>

    @Throws(IllegalStateException::class)
    fun getNativeTokenBalance(networkId: String): ProxyAmount?

    /**
     * @param networkId
     * @return currency name
     */
    @Throws(IllegalStateException::class)
    fun getNetworkCurrency(networkId: String): String

    /**
     * Returns selected app currency
     */
    fun getUserAppCurrency(): ProxyFiatCurrency

    @Throws(IllegalStateException::class)
    fun getLastTransactionHash(networkId: String): String?
}