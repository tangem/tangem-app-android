package com.tangem.datasource.local.quote

import androidx.datastore.core.DataStore
import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.datasource.local.quote.model.QuoteDM
import com.tangem.datasource.local.quote.model.QuotesDM
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Quote
import com.tangem.utils.extensions.orZero
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

internal class DefaultQuotesStore(
    private val dataStore: DataStore<QuotesDM>,
) : QuotesStore {

    override fun get(currenciesIds: Set<CryptoCurrency.RawID>): Flow<Set<Quote>> {
        return dataStore.data
            .map { quotes -> createQuotes(currenciesIds, quotes) }
    }

    override suspend fun getSync(currenciesIds: Set<CryptoCurrency.RawID>): Set<Quote> {
        val quotes = dataStore.data.firstOrNull().orEmpty()

        return createQuotes(currenciesIds, quotes)
    }

    private fun createQuotes(currenciesIds: Set<CryptoCurrency.RawID>, quoteEntities: Set<QuoteDM>): Set<Quote> =
        currenciesIds.mapTo(mutableSetOf()) { id ->
            quoteEntities.firstOrNull { it.rawCurrencyId == id }
                ?.let { quote ->
                    Quote.Value(
                        rawCurrencyId = id,
                        fiatRate = quote.fiatRate,
                        priceChange = quote.priceChange,
                    )
                }
                ?: Quote.Empty(id)
        }

    override suspend fun store(response: QuotesResponse) {
        val newQuotes = response.quotes.mapTo(mutableSetOf()) { (currencyId, quote) ->
            QuoteDM(
                rawCurrencyId = CryptoCurrency.RawID(currencyId),
                fiatRate = quote.price.orZero(),
                priceChange = quote.priceChange24h.orZero().movePointLeft(2),
            )
        }

        dataStore.updateData { storedQuotes ->
            (newQuotes + storedQuotes).distinctBy { it.rawCurrencyId }.toSet()
        }
    }
}