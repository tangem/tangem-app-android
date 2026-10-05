package com.tangem.domain.offramp.repository

import com.tangem.domain.core.lce.Lce
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.offramp.model.PendingOfframp
import kotlinx.coroutines.flow.StateFlow

typealias OfframpInitializationStatus = Lce<Throwable, Any>

/**
 * Repository for offramp (sell crypto) operations.
 *
 * [availableForSell], and [getOfframpUrl] for a mainnet currency, rely on the sell listing fetched by [update] -
 * until [update] has completed at least once, they return `false`/`null` rather than throwing or suspending. A
 * testnet currency's [getOfframpUrl] does not need [update]: it returns a blockchain-specific testnet top-up URL
 * without consulting the listing.
 */
interface OfframpRepository {

    val initializationStatus: StateFlow<OfframpInitializationStatus>

    suspend fun update()

    /**
     * Whether [cryptoCurrency] can currently be sold through the offramp provider.
     */
    fun availableForSell(cryptoCurrency: CryptoCurrency): Boolean

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

    /**
     * Returns every stored app-initiated sell — **including expired ones** that have not been pruned yet (read-only;
     * does not mutate the store). Use [PendingOfframp.isExpired] to tell them apart.
     *
     * Intended for QA/tester tooling that needs to reproduce a returning `redirect_sell` deeplink from a real,
     * app-registered sell and to inspect stale records.
     */
    suspend fun getAllStoredOfframps(): List<PendingOfframp>
}