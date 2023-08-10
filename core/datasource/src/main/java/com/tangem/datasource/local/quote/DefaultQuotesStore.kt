package com.tangem.datasource.local.quote

import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.quote.model.StoredQuote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal class DefaultQuotesStore(
    private val dataStore: StringKeyDataStore<StoredQuote>,
) : QuotesStore {

    override fun get(rawCurrenciesIds: Set<String>): Flow<Set<StoredQuote>> {
        val flows = rawCurrenciesIds.map { rawCurrencyId ->
            dataStore.get(rawCurrencyId)
        }

        return combine(flows) { quotes -> quotes.toSet() }
    }

    override suspend fun store(response: QuotesResponse) {
        response.quotes.forEach { (rawCurrencyId, quote) ->
            dataStore.store(rawCurrencyId, StoredQuote(rawCurrencyId, quote))
        }
    }
}
