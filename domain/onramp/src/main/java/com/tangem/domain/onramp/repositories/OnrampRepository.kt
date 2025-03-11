package com.tangem.domain.onramp.repositories

import com.tangem.domain.onramp.model.*
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
interface OnrampRepository {
    // api
    fun getCurrencies(): Flow<List<OnrampCurrency>>
    fun getCountries(): Flow<List<OnrampCountry>>
    suspend fun getCountriesSync(): List<OnrampCountry>?
    suspend fun getCountryByIp(): OnrampCountry
    suspend fun getStatus(txId: String): OnrampStatus
    suspend fun fetchCurrencies()
    suspend fun fetchCountries(): List<OnrampCountry>
    suspend fun fetchPaymentMethodsIfAbsent()
    suspend fun fetchPairs(currency: OnrampCurrency, country: OnrampCountry, cryptoCurrency: CryptoCurrency)
    suspend fun fetchQuotes(cryptoCurrency: CryptoCurrency, amount: Amount)
    suspend fun getOnrampData(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        quote: OnrampProviderWithQuote.Data,
        isDarkTheme: Boolean,
    ): OnrampTransaction

    // cache
    suspend fun saveDefaultCurrency(currency: OnrampCurrency)
    suspend fun getDefaultCurrencySync(): OnrampCurrency?
    fun getDefaultCurrency(): Flow<OnrampCurrency?>
    suspend fun saveDefaultCountry(country: OnrampCountry)
    suspend fun getDefaultCountrySync(): OnrampCountry?
    fun getDefaultCountry(): Flow<OnrampCountry?>
    suspend fun getAvailablePaymentMethods(): Set<OnrampPaymentMethod>
    suspend fun saveSelectedPaymentMethod(paymentMethod: OnrampPaymentMethod)
    fun getSelectedPaymentMethod(): Flow<OnrampPaymentMethod>
    fun getQuotes(): Flow<List<OnrampQuote>>
    suspend fun getQuotesSync(): List<OnrampQuote>?
    suspend fun clearCache()
}