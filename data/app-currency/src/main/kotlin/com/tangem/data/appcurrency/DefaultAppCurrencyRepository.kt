package com.tangem.data.appcurrency

import com.tangem.data.appcurrency.utils.AppCurrencyConverter
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.CurrenciesResponse
import com.tangem.datasource.local.appcurrency.AvailableAppCurrenciesStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObject
import com.tangem.datasource.local.preferences.utils.getSyncOrNull
import com.tangem.datasource.local.preferences.utils.storeObject
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.Duration

internal class DefaultAppCurrencyRepository(
    private val tangemTechApi: TangemTechApi,
    private val appPreferencesStore: AppPreferencesStore,
    private val availableAppCurrenciesStore: AvailableAppCurrenciesStore,
    private val cacheRegistry: CacheRegistry,
    private val dispatchers: CoroutineDispatcherProvider,
) : AppCurrencyRepository {

    private val appCurrencyConverter = AppCurrencyConverter()

    override fun getSelectedAppCurrency(): Flow<AppCurrency> {
        return channelFlow {
            launch {
                appPreferencesStore
                    .getObject<CurrenciesResponse.Currency>(key = PreferencesKeys.SELECTED_APP_CURRENCY_KEY)
                    .filterNotNull()
                    .map(appCurrencyConverter::convert)
                    .collect(::send)
            }

            withContext(dispatchers.io) {
                if (appPreferencesStore.getSyncOrNull(PreferencesKeys.SELECTED_APP_CURRENCY_KEY) == null) {
                    fetchDefaultAppCurrency()
                }
            }
        }
    }

    override suspend fun getAvailableAppCurrencies(): List<AppCurrency> {
        return withContext(dispatchers.io) {
            fetchAvailableCurrenciesIfExpired()

            val currencies = availableAppCurrenciesStore.getAllSyncOrNull()
                ?.map(appCurrencyConverter::convert)
                ?.sortedBy(AppCurrency::name)

            requireNotNull(currencies) {
                "No available currencies stored"
            }
        }
    }

    override suspend fun changeAppCurrency(currencyCode: String) {
        withContext(dispatchers.io) {
            val currency = requireNotNull(availableAppCurrenciesStore.getSyncOrNull(currencyCode)) {
                "Unable to find app currency with provided code: $currencyCode"
            }

            appPreferencesStore.storeObject(
                key = PreferencesKeys.SELECTED_APP_CURRENCY_KEY,
                value = currency,
            )
        }
    }

    override suspend fun fetchDefaultAppCurrency(isRefresh: Boolean) {
        withContext(dispatchers.io) {
            fetchAvailableCurrenciesIfExpired(isRefresh)
            val appCurrency = appPreferencesStore.getSyncOrNull(PreferencesKeys.SELECTED_APP_CURRENCY_KEY)
            changeAppCurrency(appCurrency ?: DEFAULT_CURRENCY_CODE)
        }
    }

    private suspend fun fetchAvailableCurrenciesIfExpired(isRefresh: Boolean = false) {
        cacheRegistry.invokeOnExpire(
            key = AVAILABLE_CURRENCIES_CACHE_KEY,
            skipCache = isRefresh,
            expireIn = Duration.standardMinutes(AVAILABLE_CURRENCIES_CACHE_KEY_EXPIRE_MINUTES),
            block = { fetchAvailableCurrencies() },
        )
    }

    private suspend fun fetchAvailableCurrencies() {
        val response = safeApiCall(
            call = { tangemTechApi.getCurrencyList().bind() },
            onError = {
                cacheRegistry.invalidate(AVAILABLE_CURRENCIES_CACHE_KEY)
                getDefaultCurrenciesResponse()
            },
        )

        availableAppCurrenciesStore.store(response)
    }

    private fun getDefaultCurrenciesResponse(): CurrenciesResponse = CurrenciesResponse(
        currencies = listOf(
            CurrenciesResponse.Currency(
                id = DEFAULT_CURRENCY_CODE.lowercase(),
                code = DEFAULT_CURRENCY_CODE,
                name = "US Dollar",
                unit = "$",
                type = "fiat",
                rateBTC = "",
            ),
        ),
    )

    private companion object {
        const val AVAILABLE_CURRENCIES_CACHE_KEY = "available_currencies"
        const val AVAILABLE_CURRENCIES_CACHE_KEY_EXPIRE_MINUTES = 15L
        const val DEFAULT_CURRENCY_CODE = "USD"
    }
}