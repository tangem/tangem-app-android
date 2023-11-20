package com.tangem.datasource.local.quote

import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.quote.model.StoredQuote
import com.tangem.domain.tokens.model.CryptoCurrency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

internal class DefaultQuotesStore(
    private val dataStore: StringKeyDataStore<StoredQuote>,
) : QuotesStore {

    override fun get(currenciesIds: Set<CryptoCurrency.ID>): Flow<Set<StoredQuote>> {
        return channelFlow {
            if (dataStore.isEmpty()) {
                send(emptySet())
            }

            val quotes = currenciesIds.mapNotNull { currencyId ->
                currencyId.rawCurrencyId?.let {
                    dataStore.getSyncOrNull(it)
                }
            }

            send(quotes.toSet())
        }
    }

    override suspend fun getSync(currenciesIds: Set<CryptoCurrency.ID>): Set<StoredQuote> {
        return currenciesIds.mapNotNull { currencyId ->
            currencyId.rawCurrencyId?.let {
                dataStore.getSyncOrNull(it)
            }
        }.toSet()
    }

    override suspend fun store(response: QuotesResponse) {
        val quotes = response.quotes.mapValues { (id, quote) ->
            StoredQuote(id, quote)
        }

        dataStore.store(quotes)
    }
}