package com.tangem.domain.offramp.repository

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.offramp.model.PendingOfframp

/**
 * Repository for offramp (sell crypto) operations
 */
interface OfframpRepository {

    /**
     * Get offramp (sell) URL for the given cryptocurrency
     *
     * @param cryptoCurrency    crypto currency to sell
     * @param fiatCurrencyCode  fiat currency code (e.g., "USD", "EUR")
     * @param walletAddress     wallet address for the refund
     * @param requestId         single-use nonce embedded into the provider redirect URL to authenticate the
     *                          returning `redirect_sell` deeplink
     * @return URL for offramp service or null if not available
     */
    fun getOfframpUrl(
        cryptoCurrency: CryptoCurrency,
        fiatCurrencyCode: String,
        walletAddress: String,
        requestId: String,
    ): String?

    /**
     * Registers a new app-initiated sell for [userWalletId] / [currencyId], prunes expired records, and returns a
     * fresh single-use `request_id` to embed in the provider redirect URL.
     */
    suspend fun registerPendingOfframp(userWalletId: UserWalletId, currencyId: String): String

    /**
     * Returns and removes (single-use) the pending sell matching [requestId] only when it is not expired and was
     * registered for the same [userWalletId] and [currencyId]. Returns `null` otherwise, leaving a non-matching
     * record untouched so a tampered redirect cannot burn a legitimate pending sell.
     */
    suspend fun consumePendingOfframp(
        requestId: String,
        userWalletId: UserWalletId,
        currencyId: String,
    ): PendingOfframp?
}