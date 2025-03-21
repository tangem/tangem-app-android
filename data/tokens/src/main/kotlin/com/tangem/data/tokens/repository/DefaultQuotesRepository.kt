package com.tangem.data.tokens.repository

import com.tangem.data.common.api.safeApiCallWithTimeout
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.tokens.utils.QuotesConverter
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

internal class DefaultQuotesRepository(
    private val tangemTechApi: TangemTechApi,
    private val appPreferencesStore: AppPreferencesStore,
    private val quotesStore: QuotesStore,
    private val cacheRegistry: CacheRegistry,
    private val dispatchers: CoroutineDispatcherProvider,
) : QuotesRepository {

    private val quotesConverter = QuotesConverter()
    private val quotesUnsupportedCurrenciesAdapter = QuotesUnsupportedCurrenciesIdAdapter()

    @Volatile
    private var quotesFetchedForAppCurrency: String? = null
    private val mutex = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getQuotesUpdates(currenciesIds: Set<CryptoCurrency.ID>, refresh: Boolean): Flow<Set<Quote>> {
        return appPreferencesStore.getObject<CurrenciesResponse.Currency>(
            key = PreferencesKeys.SELECTED_APP_CURRENCY_KEY,
        )
            .distinctUntilChanged()
            .filterNotNull()
            .flatMapLatest { appCurrency ->
                fetchExpiredQuotes(currenciesIds, appCurrency.id, refresh = refresh)
                quotesStore.get(currenciesIds).map { quotesConverter.convert(currenciesIds to it) }
            }
            .cancellable()
            .flowOn(dispatchers.io)
    }

    override suspend fun fetchQuotes(currenciesIds: Set<CryptoCurrency.ID>) {
        withContext(dispatchers.io) {
            val selectedAppCurrency = requireNotNull(
                value = appPreferencesStore.getObjectSyncOrNull<CurrenciesResponse.Currency>(
                    key = PreferencesKeys.SELECTED_APP_CURRENCY_KEY,
                ),
                lazyMessage = { "Unable to get selected application currency to update quotes" },
            )

            fetchExpiredQuotes(currenciesIds, selectedAppCurrency.id, true)
        }
    }

    override suspend fun getQuotesSync(currenciesIds: Set<CryptoCurrency.ID>, refresh: Boolean): Set<Quote> {
        return withContext(dispatchers.io) {
            val selectedAppCurrency = requireNotNull(
                value = appPreferencesStore.getObjectSyncOrNull<CurrenciesResponse.Currency>(
                    key = PreferencesKeys.SELECTED_APP_CURRENCY_KEY,
                ),
                lazyMessage = { "Unable to get selected application currency to update quotes" },
            )

            fetchExpiredQuotes(currenciesIds, selectedAppCurrency.id, refresh)

            val quotes = quotesStore.getSync(currenciesIds)

            quotesConverter.convert(currenciesIds to quotes)
        }
    }

    override suspend fun getQuoteSync(currencyId: CryptoCurrency.ID): Quote? {
        return withContext(dispatchers.io) {
            val setOfCurrencyId = setOf(currencyId)
            val quote = quotesStore.getSync(setOfCurrencyId).firstOrNull()
            quote?.let { quotesConverter.convert(setOfCurrencyId to setOf(it)).firstOrNull() }
        }
    }

    private suspend fun fetchExpiredQuotes(
        currenciesIds: Set<CryptoCurrency.ID>,
        appCurrencyId: String,
        refresh: Boolean,
    ) {
        // TODO("https://tangem.atlassian.net/browse/AND-6430") need refactor working with quotesFetchedForAppCurrency,
        //  it changes after filterExpiredCurrenciesIds
        //  calls with different coroutines and lead to fetchQuotes
        mutex.withLock {
            val expiredCurrenciesIds = filterExpiredCurrenciesIds(
                currenciesIds = currenciesIds,
                refresh = refresh || quotesFetchedForAppCurrency != appCurrencyId,
            )
            if (expiredCurrenciesIds.isEmpty()) return

            quotesFetchedForAppCurrency = appCurrencyId

            fetchQuotes(expiredCurrenciesIds, appCurrencyId)
        }
    }

    private suspend fun fetchQuotes(rawCurrenciesIds: Set<String>, appCurrencyId: String) {
        val replacementIdsResult = quotesUnsupportedCurrenciesAdapter.replaceUnsupportedCurrencies(rawCurrenciesIds)
        val response = safeApiCallWithTimeout(
            call = {
                val coinIds = replacementIdsResult.idsForRequest.joinToString(separator = ",")
                tangemTechApi.getQuotes(appCurrencyId, coinIds).bind()
            },
            onError = { error ->
                cacheRegistry.invalidate(rawCurrenciesIds.map(::getQuoteCacheKey))

                throw error
            },
        )

        val updatedResponse = quotesUnsupportedCurrenciesAdapter.getResponseWithUnsupportedCurrencies(
            response,
            replacementIdsResult.idsFiltered,
        )
        quotesStore.store(updatedResponse)
    }

    private suspend fun filterExpiredCurrenciesIds(
        currenciesIds: Set<CryptoCurrency.ID>,
        refresh: Boolean,
    ): Set<String> {
        return currenciesIds.fold(hashSetOf()) { acc, currencyId ->
            val rawCurrencyId = currencyId.rawCurrencyId
            if (rawCurrencyId != null && rawCurrencyId !in acc) {
                cacheRegistry.invokeOnExpire(
                    key = getQuoteCacheKey(rawCurrencyId),
                    skipCache = refresh,
                    block = { acc.add(rawCurrencyId) },
                )
            }
            acc
        }
    }

    private fun getQuoteCacheKey(rawCurrencyId: String): String = "quote_$rawCurrencyId"
}
