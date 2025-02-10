package com.tangem.datasource.quotes

import com.tangem.data.common.api.safeApiCallWithTimeout
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.CurrenciesResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObject
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.quote.QuotesStore
import com.tangem.datasource.quotes.conterter.QuotesConverter
import com.tangem.datasource.quotes.utils.QuotesUnsupportedCurrenciesIdAdapter
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Quote
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultQuotesDataSource @Inject constructor(
    private val tangemTechApi: TangemTechApi,
    private val appPreferencesStore: AppPreferencesStore,
    private val quotesStore: QuotesStore,
    private val cacheRegistry: CacheRegistry,
    private val dispatchers: CoroutineDispatcherProvider,
) : QuotesDataSource {

    private val quotesConverter = QuotesConverter()
    private val quotesUnsupportedCurrenciesAdapter = QuotesUnsupportedCurrenciesIdAdapter()

    @Volatile
    private var quotesFetchedForAppCurrency: String? = null
    private val mutex = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getQuotesUpdates(currenciesIds: Set<CryptoCurrency.RawID>, refresh: Boolean): Flow<Set<Quote>> {
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

    override suspend fun fetchQuotes(currenciesIds: Set<CryptoCurrency.RawID>) {
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

    override suspend fun getQuotesSync(currenciesIds: Set<CryptoCurrency.RawID>, refresh: Boolean): Set<Quote> {
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

    override suspend fun getQuoteSync(currencyId: CryptoCurrency.RawID): Quote? {
        return withContext(dispatchers.io) {
            val setOfCurrencyId = setOf(currencyId)
            val quote = quotesStore.getSync(setOfCurrencyId).firstOrNull()
            quote?.let { quotesConverter.convert(setOfCurrencyId to setOf(it)).firstOrNull() }
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
            val expiredCurrenciesIds = filterExpiredCurrenciesIds(
                currenciesIds = currenciesIds,
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
        val response = safeApiCallWithTimeout(
            call = {
                val coinIds = replacementIdsResult.idsForRequest.joinToString(separator = ",")
                tangemTechApi.getQuotes(appCurrencyId, coinIds).bind()
            },
            onError = { error ->
                cacheRegistry.invalidate(rawCurrenciesIds.map { getQuoteCacheKey(it) })

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
        currenciesIds: Set<CryptoCurrency.RawID>,
        refresh: Boolean,
    ): Set<CryptoCurrency.RawID> {
        return currenciesIds.fold(hashSetOf()) { acc, currencyId ->
            if (currencyId !in acc) {
                cacheRegistry.invokeOnExpire(
                    key = getQuoteCacheKey(currencyId),
                    skipCache = refresh,
                    block = { acc.add(currencyId) },
                )
            }
            acc
        }
    }

    private fun getQuoteCacheKey(rawCurrencyId: CryptoCurrency.RawID): String = "quote_${rawCurrencyId.value}"
}