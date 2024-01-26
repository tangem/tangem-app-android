package com.tangem.lib.crypto

import com.tangem.lib.crypto.models.Currency
import com.tangem.lib.crypto.models.ProxyAmount

/**
 * Provider for user tokens data
 */
interface UserWalletManager {

    /**
     * Returns all user tokens (merged from local and backend)
     */
    @Throws(IllegalStateException::class)
    suspend fun getUserTokens(networkId: String, derivationPath: String?, isExcludeCustom: Boolean): List<Currency>

    @Throws(IllegalStateException::class)
    fun getNativeTokenForNetwork(networkId: String): Currency

    /**
     * Returns user walletId or empty string
     */
    fun getWalletId(): String

    /**
     * Checks that token added to user wallet
     *
     * @param currency to receive referral payments
     */
    @Throws(IllegalStateException::class)
    suspend fun isTokenAdded(currency: Currency, derivationPath: String?): Boolean

    suspend fun hideAllTokens()

    /**
     * Returns wallet public address for token
     *
     * @param networkId for currency
     * @param derivationPath if null uses default
     */
    @Throws(IllegalStateException::class)
    suspend fun getWalletAddress(networkId: String, derivationPath: String?): String

    /**
     * Return balances from wallet found by networkId
     *
     * @param networkId
     * @param extraTokens tokens you want to check balance that not exists in wallet
     * @param derivationPath if null uses default
     * @return map of <Symbol, [ProxyAmount]>
     */
    @Throws(IllegalStateException::class)
    suspend fun getCurrentWalletTokensBalance(
        networkId: String,
        extraTokens: List<Currency>,
        derivationPath: String?,
    ): Map<String, ProxyAmount>

    @Throws(IllegalStateException::class)
    suspend fun getNativeTokenBalance(networkId: String, derivationPath: String?): ProxyAmount?

    /**
     * @param networkId
     * @return currency name
     */
    @Throws(IllegalStateException::class)
    fun getNetworkCurrency(networkId: String): String

    @Throws(IllegalStateException::class)
    suspend fun getLastTransactionHash(networkId: String, derivationPath: String?): String?
}
