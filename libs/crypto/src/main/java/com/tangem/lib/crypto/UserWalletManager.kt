package com.tangem.lib.crypto

import com.tangem.lib.crypto.models.ProxyAmount

/**
 * Provider for user tokens data
 */
interface UserWalletManager {

    /**
     * Returns user walletId or empty string
     */
    fun getWalletId(): String

    suspend fun hideAllTokens()

    /**
     * Returns wallet public address for token
     *
     * @param networkId for currency
     * @param derivationPath if null uses default
     */
    @Throws(IllegalStateException::class)
    suspend fun getWalletAddress(networkId: String, derivationPath: String?): String

    @Throws(IllegalStateException::class)
    suspend fun getNativeTokenBalance(networkId: String, derivationPath: String?): ProxyAmount?

    @Throws(IllegalStateException::class)
    suspend fun getLastTransactionHash(networkId: String, derivationPath: String?): String?
}