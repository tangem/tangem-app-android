package com.tangem.datasource.local.quote

import androidx.datastore.core.DataStore
import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.quote.converter.QuoteConverter
import com.tangem.domain.models.StatusSource
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Quote
import com.tangem.utils.extensions.addOrReplace
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal typealias QuotesByCurrencyId = Map<String, QuotesResponse.Quote>

/**
 * Default implementation of [QuotesStore]
 *
 * @property persistenceStore persistence quotes store
 * @property runtimeStore     runtime quotes store
 */
internal class DefaultQuotesStore(
    private val persistenceStore: DataStore<QuotesByCurrencyId>,
    private val runtimeStore: RuntimeSharedStore<Set<Quote>>,
) : QuotesStore {

    override fun get(currenciesIds: Set<CryptoCurrency.RawID>): Flow<Set<Quote>> = channelFlow {
        val cachedQuotes = getCachedQuotes(currenciesIds = currenciesIds)

        if (cachedQuotes.isNotEmpty()) {
            send(cachedQuotes)
        }

        runtimeStore.get()
            .onEach {
                val mergedQuotes = mergeQuotes(
                    currenciesIds = currenciesIds,
                    cachedQuotes = cachedQuotes,
                    runtimeQuotes = it,
                )

                send(element = mergedQuotes)
            }
            .launchIn(scope = this)
    }

    override suspend fun getSync(currenciesIds: Set<CryptoCurrency.RawID>): Set<Quote> {
        return runtimeStore.getSyncOrDefault(default = emptySet())
            .filter { it.rawCurrencyId in currenciesIds }
            .toSet()
    }

    override suspend fun store(response: QuotesResponse) {
        coroutineScope {
            launch {
                storeInRuntimeStore(
                    values = QuoteConverter(isCached = false).convertSet(input = response.quotes.entries),
                )
            }
            launch { storeInPersistenceStore(response = response) }
        }
    }

    override suspend fun storeEmptyQuotes(currenciesIds: Set<CryptoCurrency.RawID>) {
        storeInRuntimeStore(values = currenciesIds.map(Quote::Empty).toSet())
    }

    override suspend fun refresh(currenciesIds: Set<CryptoCurrency.RawID>) {
        val currentStatuses = getSync(currenciesIds)

        storeInRuntimeStore(
            values = currentStatuses.mapTo(hashSetOf()) { quote ->
                when (quote) {
                    is Quote.Empty -> quote
                    is Quote.Value -> quote.copy(source = StatusSource.CACHE)
                }
            },
        )
    }

    private suspend fun getCachedQuotes(currenciesIds: Set<CryptoCurrency.RawID>): Set<Quote.Value> {
        val ids = currenciesIds.map(CryptoCurrency.RawID::value).toSet()
        val cachedQuotes = persistenceStore.data.firstOrNull().orEmpty().filterKeys { it in ids }

        return QuoteConverter(isCached = true).convertSet(input = cachedQuotes.entries)
    }

    private fun mergeQuotes(
        currenciesIds: Set<CryptoCurrency.RawID>,
        cachedQuotes: Set<Quote.Value>,
        runtimeQuotes: Set<Quote>,
    ): Set<Quote> {
        return currenciesIds
            .mapTo(hashSetOf()) { currencyId ->
                val runtimeQuote = runtimeQuotes.firstOrNull { it.rawCurrencyId == currencyId }

                if (runtimeQuote == null || runtimeQuote is Quote.Empty) {
                    getCachedQuoteIfPossible(cachedStatuses = cachedQuotes, currencyId = currencyId)
                } else {
                    runtimeQuote
                }
            }
    }

    private fun getCachedQuoteIfPossible(cachedStatuses: Set<Quote.Value>, currencyId: CryptoCurrency.RawID): Quote {
        return cachedStatuses.firstOrNull { it.rawCurrencyId == currencyId }
            ?.copy(source = StatusSource.ONLY_CACHE)
            ?: Quote.Empty(currencyId)
    }

    private suspend fun storeInRuntimeStore(values: Set<Quote>) {
        runtimeStore.update(default = emptySet()) { saved ->
            saved.addOrReplace(items = values) { prev, new -> prev.rawCurrencyId == new.rawCurrencyId }
        }
    }

    private suspend fun storeInPersistenceStore(response: QuotesResponse) {
        persistenceStore.updateData { storedQuotes -> storedQuotes + response.quotes }
    }
}