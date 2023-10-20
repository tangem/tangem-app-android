package com.tangem.datasource.local.quote

import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.quote.model.StoredQuote
import com.tangem.domain.tokens.model.CryptoCurrency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine

internal class DefaultQuotesStore(
    private val dataStore: StringKeyDataStore<StoredQuote>,
) : QuotesStore {

    override fun get(currenciesIds: Set<CryptoCurrency.ID>): Flow<Set<StoredQuote>> {
        return channelFlow {
            val flows = currenciesIds.mapNotNull { currencyId ->
                currencyId.rawCurrencyId?.let(dataStore::get)
            }

            if (dataStore.isEmpty() || flows.isEmpty()) {
                send(emptySet())
            }

            combine(flows) { quotes -> quotes.toSet() }.collect(::send)
        }
    }

    override suspend fun store(response: QuotesResponse) {
        val quotes = response.quotes.mapValues { (id, quote) ->
            StoredQuote(id, quote)
        }

        dataStore.store(quotes)
    }
}