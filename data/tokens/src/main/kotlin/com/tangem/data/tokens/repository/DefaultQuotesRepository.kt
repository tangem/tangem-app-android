package com.tangem.data.tokens.repository

import com.tangem.data.common.api.safeApiCallWithTimeout
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.tokens.utils.QuotesUnsupportedCurrenciesIdAdapter
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.CurrenciesResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObject
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.quote.QuotesStore
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

internal class DefaultQuotesRepository(
    private val tangemTechApi: TangemTechApi,
    private val appPreferencesStore: AppPreferencesStore,
    private val quotesStore: QuotesStore,
    private val cacheRegistry: CacheRegistry,
    private val dispatchers: CoroutineDispatcherProvider,
) : QuotesRepository {

    private val quotesUnsupportedCurrenciesAdapter = QuotesUnsupportedCurrenciesIdAdapter()

    @Volatile
    private var quotesFetchedForAppCurrency: String? = null
    private val mutex = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getQuotesUpdates(currenciesIds: Set<CryptoCurrency.RawID>): Flow<Set<Quote>> {
        return appPreferencesStore.getObject<CurrenciesResponse.Currency>(
            key = PreferencesKeys.SELECTED_APP_CURRENCY_KEY,
        )
            .distinctUntilChanged()
            .filterNotNull()
            .flatMapLatest { appCurrency ->
                fetchExpiredQuotes(
                    currenciesIds = currenciesIds,
                    appCurrencyId = appCurrency.id,
                    refresh = false,
                )

                quotesStore.get(currenciesIds)
            }
            .flowOn(dispatchers.io)
    }

    override suspend fun fetchQuotes(currenciesIds: Set<CryptoCurrency.RawID>, refresh: Boolean) {
        withContext(dispatchers.io) {
            val selectedAppCurrency = requireNotNull(
                value = appPreferencesStore.getObjectSyncOrNull<CurrenciesResponse.Currency>(
                    key = PreferencesKeys.SELECTED_APP_CURRENCY_KEY,
                ),
                lazyMessage = { "Unable to get selected application currency to update quotes" },
            )

            fetchExpiredQuotes(currenciesIds, selectedAppCurrency.id, refresh)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getQuotesUpdatesLegacy(currenciesIds: Set<CryptoCurrency.RawID>, refresh: Boolean): Flow<Set<Quote>> {
        return appPreferencesStore.getObject<CurrenciesResponse.Currency>(
            key = PreferencesKeys.SELECTED_APP_CURRENCY_KEY,
        )
            .distinctUntilChanged()
            .filterNotNull()
            .flatMapLatest { appCurrency ->
                fetchExpiredQuotes(currenciesIds, appCurrency.id, refresh = refresh)
                quotesStore.get(currenciesIds)
            }
            .cancellable()
            .flowOn(dispatchers.io)
    }

    override suspend fun getQuotesSync(currenciesIds: Set<CryptoCurrency.RawID>, refresh: Boolean): Set<Quote> {
        return withContext(dispatchers.io) {
            val selectedAppCurrency = requireNotNull(
                value = appPreferencesStore.getObjectSyncOrNull<CurrenciesResponse.Currency>(
                    key = PreferencesKeys.SELECTED_APP_CURRENCY_KEY,
                ),
                lazyMessage = { "Unable to get selected application currency to update quotes" },
            )

            fetchExpiredQuotes(currenciesIds, selectedAppCurrency.id, refresh)

            quotesStore.getSync(currenciesIds)
        }
    }

    override suspend fun getQuoteSync(currencyId: CryptoCurrency.RawID): Quote? {
        return withContext(dispatchers.io) {
            val setOfCurrencyId = setOf(currencyId)

            quotesStore.getSync(setOfCurrencyId).firstOrNull()
        }
    }

    private suspend fun fetchExpiredQuotes(
        currenciesIds: Set<CryptoCurrency.RawID>,
        appCurrencyId: String,
        refresh: Boolean,
    ) {
        // TODO("[REDACTED_JIRA]") need refactor working with quotesFetchedForAppCurrency,
        //  it changes after filterExpiredCurrenciesIds
        //  calls with different coroutines and lead to fetchQuotes
        mutex.withLock {
            if (refresh) {
                quotesStore.refresh(currenciesIds)
            }

            val expiredCurrenciesIds = filterExpiredCurrenciesIds(
                currenciesIds = currenciesIds,
                appCurrencyId = appCurrencyId,
                refresh = refresh || quotesFetchedForAppCurrency != appCurrencyId,
            )
            if (expiredCurrenciesIds.isEmpty()) return

            quotesFetchedForAppCurrency = appCurrencyId

            fetchQuotes(expiredCurrenciesIds, appCurrencyId)
        }
    }

    private suspend fun fetchQuotes(rawCurrenciesIds: Set<CryptoCurrency.RawID>, appCurrencyId: String) {
        val replacementIdsResult = quotesUnsupportedCurrenciesAdapter.replaceUnsupportedCurrencies(
            rawCurrenciesIds.map { it.value }.toSet(),
        )

        safeApiCallWithTimeout(
            call = {
                val coinIds = replacementIdsResult.idsForRequest.joinToString(separator = ",")
                val response = tangemTechApi.getQuotes(appCurrencyId, coinIds).bind()

                val updatedResponse = quotesUnsupportedCurrenciesAdapter.getResponseWithUnsupportedCurrencies(
                    response = response,
                    filteredIds = replacementIdsResult.idsFiltered,
                )

                quotesStore.store(updatedResponse)
            },
            onError = { error ->
                Timber.e(error)

                cacheRegistry.invalidate(rawCurrenciesIds.map { getQuoteCacheKey(it, appCurrencyId) })
                quotesStore.storeEmptyQuotes(currenciesIds = rawCurrenciesIds)
            },
        )
    }

    private suspend fun filterExpiredCurrenciesIds(
        currenciesIds: Set<CryptoCurrency.RawID>,
        appCurrencyId: String,
        refresh: Boolean,
    ): Set<CryptoCurrency.RawID> {
        return currenciesIds.fold(hashSetOf()) { acc, currencyId ->
            if (currencyId !in acc) {
                cacheRegistry.invokeOnExpire(
                    key = getQuoteCacheKey(currencyId, appCurrencyId),
                    skipCache = refresh,
                    block = { acc.add(currencyId) },
                )
            }
            acc
        }
    }

    private fun getQuoteCacheKey(rawCurrencyId: CryptoCurrency.RawID, appCurrencyId: String): String {
        return "quote_${rawCurrencyId.value}_$appCurrencyId"
    }
}