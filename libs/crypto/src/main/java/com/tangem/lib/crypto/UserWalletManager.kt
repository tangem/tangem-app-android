package com.tangem.lib.crypto

import com.tangem.lib.crypto.models.Currency

/**
 * Provider for user tokens data
 */
interface UserWalletManager {

    /**
     * Returns all user tokens (merged from local and backend)
     */
    suspend fun getUserTokens(): List<Currency>

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
}
