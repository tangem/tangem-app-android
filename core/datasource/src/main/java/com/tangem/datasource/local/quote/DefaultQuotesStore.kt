package com.tangem.datasource.local.quote

import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.quote.model.StoredQuote
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.utils.extensions.addOrReplace
import kotlinx.coroutines.flow.*

internal class DefaultQuotesStore(
    private val dataStore: StringKeyDataStore<StoredQuote>,
) : QuotesStore {

    override fun get(currenciesIds: Set<CryptoCurrency.RawID>): Flow<Set<StoredQuote>> {
        return channelFlow {
            val flows = currenciesIds.map { currencyId -> dataStore.get(currencyId.value) }

            if (dataStore.isEmpty() || flows.isEmpty()) {
                send(emptySet())
            }

            merge(*flows.toTypedArray())
                .scan<StoredQuote, Set<StoredQuote>>(emptySet()) { acc, quote ->
                    acc.addOrReplace(quote) { it.rawCurrencyId == quote.rawCurrencyId }
                }
                .filter(Set<StoredQuote>::isNotEmpty)
                .collect(::send)
        }
    }

    override suspend fun getSync(currenciesIds: Set<CryptoCurrency.RawID>): Set<StoredQuote> {
        return currenciesIds.mapNotNull { currencyId -> dataStore.getSyncOrNull(currencyId.value) }.toSet()
    }

    override suspend fun store(response: QuotesResponse) {
        val quotes = response.quotes.mapValues { (id, quote) ->
            StoredQuote(id, quote)
        }

        dataStore.store(quotes)
    }
}