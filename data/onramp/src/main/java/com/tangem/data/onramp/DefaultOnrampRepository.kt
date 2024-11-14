package com.tangem.data.onramp

import com.tangem.data.onramp.converters.CountryConverter
import com.tangem.data.onramp.converters.CurrencyConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.onramp.OnrampApi
import com.tangem.datasource.api.onramp.models.response.model.OnrampCountryDTO
import com.tangem.datasource.api.onramp.models.response.model.OnrampCurrencyDTO
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObject
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.preferences.utils.storeObject
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class DefaultOnrampRepository(
    private val onrampApi: OnrampApi,
    private val dispatchers: CoroutineDispatcherProvider,
    private val appPreferencesStore: AppPreferencesStore,
) : OnrampRepository {

    private val currencyConverter = CurrencyConverter()
    private val countryConverter = CountryConverter(currencyConverter)

    override suspend fun getCurrencies(): List<OnrampCurrency> = withContext(dispatchers.io) {
        onrampApi.getCurrencies()
            .getOrThrow()
            .map(currencyConverter::convert)
    }

    override suspend fun getCountries(): List<OnrampCountry> = withContext(dispatchers.io) {
        onrampApi.getCountries()
            .getOrThrow()
            .map(countryConverter::convert)
    }

    override suspend fun getCountryByIp(): OnrampCountry = withContext(dispatchers.io) {
        onrampApi.getCountryByIp()
            .getOrThrow()
            .let(countryConverter::convert)
    }

    override suspend fun saveDefaultCurrency(currency: OnrampCurrency) = withContext(dispatchers.io) {
        appPreferencesStore.storeObject<OnrampCurrencyDTO>(
            key = PreferencesKeys.ONRAMP_DEFAULT_CURRENCY,
            value = currencyConverter.convertBack(currency),
        )
    }

    override suspend fun getDefaultCurrencySync(): OnrampCurrency? = withContext(dispatchers.io) {
        appPreferencesStore
            .getObjectSyncOrNull<OnrampCurrencyDTO>(PreferencesKeys.ONRAMP_DEFAULT_CURRENCY)
            ?.let(currencyConverter::convert)
    }

    override fun getDefaultCurrency(): Flow<OnrampCurrency?> {
        return appPreferencesStore
            .getObject<OnrampCurrencyDTO>(PreferencesKeys.ONRAMP_DEFAULT_CURRENCY)
            .map { it?.let(currencyConverter::convert) }
    }

    override suspend fun saveDefaultCountry(country: OnrampCountry) = withContext(dispatchers.io) {
        appPreferencesStore.storeObject<OnrampCountryDTO>(
            key = PreferencesKeys.ONRAMP_DEFAULT_COUNTRY,
            value = countryConverter.convertBack(country),
        )
    }

    override suspend fun getDefaultCountrySync(): OnrampCountry? = withContext(dispatchers.io) {
        appPreferencesStore
            .getObjectSyncOrNull<OnrampCountryDTO>(PreferencesKeys.ONRAMP_DEFAULT_COUNTRY)
            ?.let(countryConverter::convert)
    }

    override fun getDefaultCountry(): Flow<OnrampCountry?> {
        return appPreferencesStore
            .getObject<OnrampCountryDTO>(PreferencesKeys.ONRAMP_DEFAULT_COUNTRY)
            .map { it?.let(countryConverter::convert) }
    }
}