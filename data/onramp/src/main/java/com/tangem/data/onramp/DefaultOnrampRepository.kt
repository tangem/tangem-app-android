package com.tangem.data.onramp

import com.tangem.data.onramp.converters.CountryConverter
import com.tangem.data.onramp.converters.CurrencyConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.onramp.OnrampApi
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

internal class DefaultOnrampRepository(
    private val onrampApi: OnrampApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : OnrampRepository {

    private val currencyConverter = CurrencyConverter()
    private val countryConverter = CountryConverter(currencyConverter)

    override suspend fun fetchCurrencies(): List<OnrampCurrency> = withContext(dispatchers.io) {
        return@withContext onrampApi.getCurrencies()
            .getOrThrow()
            .map(currencyConverter::convert)
    }

    override suspend fun fetchCountries(): List<OnrampCountry> = withContext(dispatchers.io) {
        return@withContext onrampApi.getCountries()
            .getOrThrow()
            .map(countryConverter::convert)
    }
}
