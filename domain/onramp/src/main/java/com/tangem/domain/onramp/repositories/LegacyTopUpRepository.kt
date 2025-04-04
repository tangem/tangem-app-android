package com.tangem.domain.onramp.repositories

import com.tangem.domain.tokens.model.CryptoCurrency

/**
 * Support legacy onboarding note/twins to get top up URL from mercuryo
 *
 */
interface LegacyTopUpRepository {

    /**
     * Get top up URL from mercuryo
     *
     * @property cryptoCurrency         crypto currency
     * @property walletAddress          address of wallet
     */
    suspend fun getTopUpUrl(cryptoCurrency: CryptoCurrency, walletAddress: String): String

    companion object {
        const val SCHEME = "https"
        const val SUCCESS_URL = "https://tangem.com/success"
    }
}