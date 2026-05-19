package com.tangem.data.quotes.multi

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.data.common.quote.QuotesFetcher
import com.tangem.data.common.quote.QuotesFetcher.Field
import com.tangem.data.quotes.store.QuotesStatusesStore
import com.tangem.data.quotes.store.setSourceAsCache
import com.tangem.data.quotes.store.setSourceAsOnlyCache
import com.tangem.data.quotes.utils.QuotesUnsupportedCurrenciesIdAdapter
import com.tangem.datasource.api.tangemTech.models.CurrenciesResponse
import com.tangem.datasource.appcurrency.AppCurrencyResponseStore
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.FiatCurrency
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [MultiQuoteStatusFetcher]
 *
 * @property quotesFetcher            quotes fetcher
 * @property appCurrencyResponseStore app currency response store
 * @property quotesStatusesStore      quotes statuses store
 * @property dispatchers              dispatchers
 *
[REDACTED_AUTHOR]
 */
@Singleton
internal class DefaultMultiQuoteStatusFetcher @Inject constructor(
    private val quotesFetcher: QuotesFetcher,
    private val appCurrencyResponseStore: AppCurrencyResponseStore,
    private val quotesStatusesStore: QuotesStatusesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : MultiQuoteStatusFetcher {

    override suspend fun invoke(params: MultiQuoteStatusFetcher.Params) = Either.catchOn(dispatchers.default) {
        if (params.currenciesIds.isEmpty()) {
            TangemLogger.d("No currencies to fetch quotes for")
            return@catchOn
        }

        quotesStatusesStore.setSourceAsCache(currenciesIds = params.currenciesIds)

        val replacementIdsResult = QuotesUnsupportedCurrenciesIdAdapter.replaceUnsupportedCurrencies(
            currenciesIds = params.currenciesIds.mapTo(
                destination = hashSetOf(),
                transform = CryptoCurrency.RawID::value,
            ),
        )

        val (fiatCurrencyId, fiatCurrency) = resolveFiatCurrency()

        val response = quotesFetcher.fetch(
            fiatCurrencyId = fiatCurrencyId,
            currenciesIds = replacementIdsResult.idsForRequest,
            fields = setOf(Field.PRICE, Field.PRICE_CHANGE_24H, Field.PRICE_USD),
        )
            .getOrElse { error("Cause: $it") }

        val updatedResponse = QuotesUnsupportedCurrenciesIdAdapter.getResponseWithUnsupportedCurrencies(
            response = response,
            filteredIds = replacementIdsResult.idsFiltered,
        )

        quotesStatusesStore.store(values = updatedResponse.quotes, fiatCurrency = fiatCurrency)
    }
        .onLeft { throwable ->
            TangemLogger.e("Error", throwable)
            quotesStatusesStore.setSourceAsOnlyCache(currenciesIds = params.currenciesIds)
        }

    private suspend fun resolveFiatCurrency(): Pair<String, FiatCurrency> {
        val stored = appCurrencyResponseStore.getSyncOrNull() ?: failOnMissingAppCurrency()
        if (stored.id.isBlank()) failOnMissingAppCurrency()

        return stored.id to stored.toFiatCurrency()
    }

    private fun failOnMissingAppCurrency(): Nothing {
        val exception = IllegalStateException("Unable to get AppCurrency for updating quotes")
        TangemLogger.e("Error", exception)
        throw exception
    }

    private fun CurrenciesResponse.Currency.toFiatCurrency(): FiatCurrency {
        return FiatCurrency(code = code, symbol = unit)
    }
}