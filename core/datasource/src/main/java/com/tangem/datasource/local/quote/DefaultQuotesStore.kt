package com.tangem.datasource.local.quote

import androidx.datastore.core.DataStore
import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.quote.converter.QuoteConverter
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Quote
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
                val mergedQuotes = mergeQuotes(cachedQuotes = cachedQuotes, runtimeQuotes = it)
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
            launch { storeInRuntimeStore(response = response) }
            launch { storeInPersistenceStore(response = response) }
        }
    }

    private suspend fun getCachedQuotes(currenciesIds: Set<CryptoCurrency.RawID>): Set<Quote.Value> {
        val ids = currenciesIds.map(CryptoCurrency.RawID::value).toSet()
        val cachedQuotes = persistenceStore.data.firstOrNull().orEmpty().filterKeys { it in ids }

        return QuoteConverter(isCached = true).convertSet(input = cachedQuotes.entries)
    }

    private fun mergeQuotes(cachedQuotes: Set<Quote.Value>, runtimeQuotes: Set<Quote>): Set<Quote> {
        return runtimeQuotes.map { runtimeQuote ->
            if (runtimeQuote is Quote.Empty) {
                cachedQuotes.firstOrNull { runtimeQuote.rawCurrencyId == it.rawCurrencyId } ?: runtimeQuote
            } else {
                runtimeQuote
            }
        }
            .toSet()
    }

    private suspend fun storeInRuntimeStore(response: QuotesResponse) {
        val new = QuoteConverter(isCached = false).convertSet(input = response.quotes.entries)

        runtimeStore.update(default = emptySet()) { saved ->
            (saved + new).distinctBy { it.rawCurrencyId }.toSet()
        }
    }

    private suspend fun storeInPersistenceStore(response: QuotesResponse) {
        persistenceStore.updateData { storedQuotes -> storedQuotes + response.quotes }
    }
}