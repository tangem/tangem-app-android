package com.tangem.data.tokens.repository

import com.tangem.data.common.api.safeApiCall
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.tokens.utils.QuotesConverter
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.appcurrency.SelectedAppCurrencyStore
import com.tangem.datasource.local.quote.QuotesStore
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Quote
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class DefaultQuotesRepository(
    private val tangemTechApi: TangemTechApi,
    private val quotesStore: QuotesStore,
    private val selectedAppCurrencyStore: SelectedAppCurrencyStore,
    private val cacheRegistry: CacheRegistry,
    private val dispatchers: CoroutineDispatcherProvider,
) : QuotesRepository {

    private val quotesConverter = QuotesConverter()

    @Volatile
    private var quotesFetchedForAppCurrency: String? = null

    override fun getQuotesUpdates(currenciesIds: Set<CryptoCurrency.ID>): Flow<Set<Quote>> = channelFlow {
        launch(dispatchers.io) {
            quotesStore.get(currenciesIds)
                .map(quotesConverter::convertSet)
                .collectLatest(::send)
        }

        withContext(dispatchers.io) {
            selectedAppCurrencyStore.get()
                .distinctUntilChanged()
                .collectLatest { appCurrency ->
                    fetchExpiredQuotes(currenciesIds, appCurrency.id, refresh = false)
                }
        }
    }.cancellable()

    override suspend fun getQuotesSync(currenciesIds: Set<CryptoCurrency.ID>, refresh: Boolean): Set<Quote> {
        return withContext(dispatchers.io) {
            val selectedAppCurrency = requireNotNull(selectedAppCurrencyStore.getSyncOrNull()) {
                "Unable to get selected application currency to update quotes"
            }

            fetchExpiredQuotes(currenciesIds, selectedAppCurrency.id, refresh)

            val quotes = quotesStore.get(currenciesIds).first()

            quotesConverter.convertSet(quotes)
        }
    }

    private suspend fun fetchExpiredQuotes(
        currenciesIds: Set<CryptoCurrency.ID>,
        appCurrencyId: String,
        refresh: Boolean,
    ) {
        val expiredCurrenciesIds = filterExpiredCurrenciesIds(
            currenciesIds = currenciesIds,
            refresh = refresh || quotesFetchedForAppCurrency != appCurrencyId,
        )
        if (expiredCurrenciesIds.isEmpty()) return

        quotesFetchedForAppCurrency = appCurrencyId

        fetchQuotes(expiredCurrenciesIds, appCurrencyId)
    }

    private suspend fun fetchQuotes(rawCurrenciesIds: Set<String>, appCurrencyId: String) {
        val response = safeApiCall(
            call = {
                val coinIds = rawCurrenciesIds.joinToString(separator = ",")
                tangemTechApi.getQuotes(appCurrencyId, coinIds).bind()
            },
            onError = {
                cacheRegistry.invalidate(rawCurrenciesIds.map(::getQuoteCacheKey))
                null
            },
        )

        if (response != null) {
            quotesStore.store(response)
        }
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