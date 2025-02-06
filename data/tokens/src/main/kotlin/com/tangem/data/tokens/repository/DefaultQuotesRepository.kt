package com.tangem.data.tokens.repository

import com.tangem.data.common.api.safeApiCallWithTimeout
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.tokens.entity.QuoteDM
import com.tangem.data.tokens.utils.QuotesConverter
import com.tangem.data.tokens.utils.QuotesUnsupportedCurrenciesIdAdapter
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.CurrenciesResponse
import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObject
import com.tangem.datasource.local.preferences.utils.getObjectSet
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.quote.QuotesStore
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.orZero
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

    override fun getQuotesUpdates(currenciesIds: Set<CryptoCurrency.RawID>): Flow<Set<Quote>> {
        return appPreferencesStore
            .getObjectSet<QuoteDM>(PreferencesKeys.QUOTES_KEY)
            .map { quotes ->
                currenciesIds.mapTo(hashSetOf()) { id ->
                    quotes.firstOrNull { it.rawCurrencyId == id }
                        ?.let { quote ->
                            Quote.Value(
                                rawCurrencyId = id,
                                fiatRate = quote.fiatRate,
                                priceChange = quote.priceChange,
                            )
                        }
                        ?: Quote.Empty(id)
                }
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
                quotesStore.get(currenciesIds).map { quotesConverter.convert(currenciesIds to it) }
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
        storeQuotes(updatedResponse.quotes)
    }

    private suspend fun storeQuotes(quotes: Map<String, QuotesResponse.Quote>) {
        val newQuotes = quotes.mapTo(mutableSetOf()) { (currencyId, quote) ->
            QuoteDM(
                rawCurrencyId = CryptoCurrency.RawID(currencyId),
                fiatRate = quote.price.orZero(),
                priceChange = quote.priceChange24h.orZero(),
            )
        }
        val key = PreferencesKeys.QUOTES_KEY

        appPreferencesStore.editData { preferences ->
            val storedQuotes = preferences.getObjectSet<QuoteDM>(key) ?: emptySet()
            // Replace old quotes with new ones and discard duplicates
            val updatedQuotes = (newQuotes + storedQuotes).distinctBy { it.rawCurrencyId }.toSet()

            preferences.setObjectSet(key, updatedQuotes)
        }
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