package com.tangem.datasource.local.quote

import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.quote.model.StoredQuote
import com.tangem.domain.tokens.models.CryptoCurrency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal class DefaultQuotesStore(
    private val dataStore: StringKeyDataStore<StoredQuote>,
) : QuotesStore {

    override fun get(currenciesIds: Set<CryptoCurrency.ID>): Flow<Set<StoredQuote>> {
        val flows = currenciesIds.mapNotNull { currencyId ->
            dataStore.get(currencyId.rawCurrencyId ?: return@mapNotNull null)
        }

        return combine(flows) { quotes -> quotes.toSet() }
    }

    override suspend fun store(response: QuotesResponse) {
        response.quotes.forEach { (rawCurrencyId, quote) ->
            dataStore.store(rawCurrencyId, StoredQuote(rawCurrencyId, quote))
        }
    }
}
