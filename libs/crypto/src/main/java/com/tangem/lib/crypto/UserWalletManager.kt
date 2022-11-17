package com.tangem.lib.crypto

import com.tangem.lib.crypto.models.Token

/**
 * Provider for user tokens data
 */
interface UserWalletManager {

    /**
     * Returns all user tokens (merged from local and backend)
     */
    suspend fun getUserTokens(): List<Token>

    /**
     * Returns user walletId
     */
    fun getWalletId(): String

    /**
     * Checks that token added to user wallet
     *
     * @param token to receive referral payments
     */
    suspend fun isTokenAdded(token: Token): Boolean

    /**
     * Adds token to wallet if its not
     *
     * @param token to add to wallet
     */
    fun addToken(token: Token)

    /**
     * Returns wallet public address for token
     *
     * @param token for which find address
     */
    fun getWalletAddress(token: Token): String
}