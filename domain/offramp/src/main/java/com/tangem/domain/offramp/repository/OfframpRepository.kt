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
     * @param requestId         nonce embedded into the provider redirect URL to authenticate the returning
     *                          `redirect_sell` deeplink
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
     * fresh `request_id` to embed in the provider redirect URL. The record stays valid until it expires.
     */
    suspend fun registerPendingOfframp(userWalletId: UserWalletId, currencyId: String): String

    /**
     * Returns the pending sell matching [requestId] when it is not expired and was registered for the same
     * [userWalletId] and [currencyId]; returns `null` otherwise.
     *
     * The matching record is **not** removed — it remains valid until it expires, so the same `redirect_sell`
     * deeplink can be followed repeatedly within that window (e.g. the user re-opens it). Expired records are pruned
     * as a side effect.
     */
    suspend fun resolvePendingOfframp(
        requestId: String,
        userWalletId: UserWalletId,
        currencyId: String,
    ): PendingOfframp?
}