package com.tangem.domain.onramp.repositories

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.onramp.model.*
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.wallets.models.UserWallet
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
interface OnrampRepository {
    // api
    fun getCurrencies(): Flow<List<OnrampCurrency>>
    fun getCountries(): Flow<List<OnrampCountry>>
    suspend fun getCountriesSync(): List<OnrampCountry>?
    suspend fun getCountryByIp(userWallet: UserWallet): OnrampCountry
    suspend fun getStatus(userWallet: UserWallet, txId: String): OnrampStatus
    suspend fun fetchCurrencies(userWallet: UserWallet)
    suspend fun fetchCountries(userWallet: UserWallet): List<OnrampCountry>
    suspend fun fetchPaymentMethodsIfAbsent(userWallet: UserWallet)
    suspend fun fetchPairs(
        userWallet: UserWallet,
        currency: OnrampCurrency,
        country: OnrampCountry,
        cryptoCurrency: CryptoCurrency,
    )

    suspend fun fetchQuotes(userWallet: UserWallet, cryptoCurrency: CryptoCurrency, amount: Amount)
    suspend fun getOnrampData(
        userWallet: UserWallet,
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
    fun getQuotes(): Flow<List<OnrampQuote>>
    suspend fun getQuotesSync(): List<OnrampQuote>?
    suspend fun clearCache()
}