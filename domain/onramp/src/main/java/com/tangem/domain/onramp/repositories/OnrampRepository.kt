package com.tangem.domain.onramp.repositories

import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.domain.tokens.model.CryptoCurrency
import kotlinx.coroutines.flow.Flow

interface OnrampRepository {
    // api
    suspend fun getCurrencies(): List<OnrampCurrency>
    suspend fun getCountries(): List<OnrampCountry>
    suspend fun getCountryByIp(): OnrampCountry
    suspend fun getStatus(txId: String): OnrampStatus
    suspend fun fetchPaymentMethodsIfAbsent()
    suspend fun fetchPairs(currency: OnrampCurrency, country: OnrampCountry, cryptoCurrency: CryptoCurrency)
    suspend fun fetchQuotes(
        currency: OnrampCurrency,
        country: OnrampCountry,
        cryptoCurrency: CryptoCurrency,
        fromAmount: String,
    )

    // cache
    suspend fun saveDefaultCurrency(currency: OnrampCurrency)
    suspend fun getDefaultCurrencySync(): OnrampCurrency?
    fun getDefaultCurrency(): Flow<OnrampCurrency?>
    suspend fun saveDefaultCountry(country: OnrampCountry)
    suspend fun getDefaultCountrySync(): OnrampCountry?
    fun getDefaultCountry(): Flow<OnrampCountry?>
    suspend fun getPaymentMethods(): List<OnrampPaymentMethod>
    suspend fun clearCache()
}
