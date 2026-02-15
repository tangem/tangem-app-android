package com.tangem.domain.offramp.repository

import com.tangem.domain.models.currency.CryptoCurrency

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
     * @return URL for offramp service or null if not available
     */
    fun getOfframpUrl(cryptoCurrency: CryptoCurrency, fiatCurrencyCode: String, walletAddress: String): String?
}