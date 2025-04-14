package com.tangem.data.quotes.multi

import arrow.core.Either
import com.tangem.data.common.api.safeApiCallWithTimeout
import com.tangem.data.quotes.store.QuotesStoreV2
import com.tangem.data.tokens.utils.QuotesUnsupportedCurrenciesIdAdapter
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.appcurrency.AppCurrencyResponseStore
import com.tangem.domain.quotes.multi.MultiQuoteFetcher
import com.tangem.domain.tokens.model.CryptoCurrency
import timber.log.Timber

/**
 * Default implementation of [MultiQuoteFetcher]
 *
 * @property tangemTechApi            tangemTech api
 * @property appCurrencyResponseStore app currency response store
 * @property quotesStore              quotes store
 *
[REDACTED_AUTHOR]
 */
internal class DefaultMultiQuoteFetcher(
    private val tangemTechApi: TangemTechApi,
    private val appCurrencyResponseStore: AppCurrencyResponseStore,
    private val quotesStore: QuotesStoreV2,
) : MultiQuoteFetcher {

    private val quotesUnsupportedCurrenciesAdapter = QuotesUnsupportedCurrenciesIdAdapter()

    override suspend fun invoke(params: MultiQuoteFetcher.Params): Either<Throwable, Unit> = Either.catch {
        quotesStore.refresh(currenciesIds = params.currenciesIds)

        val replacementIdsResult = quotesUnsupportedCurrenciesAdapter.replaceUnsupportedCurrencies(
            currenciesIds = params.currenciesIds.mapTo(
                destination = hashSetOf(),
                transform = CryptoCurrency.RawID::value,
            ),
        )

        val appCurrency = appCurrencyResponseStore.getSyncOrNull()
            ?: error(message = "Unable to get AppCurrency for updating quotes")

        safeApiCallWithTimeout(
            call = {
                val coinIds = replacementIdsResult.idsForRequest.joinToString(separator = ",")
                val response = tangemTechApi.getQuotes(currencyId = appCurrency.id, coinIds = coinIds).bind()

                val updatedResponse = quotesUnsupportedCurrenciesAdapter.getResponseWithUnsupportedCurrencies(
                    response = response,
                    filteredIds = replacementIdsResult.idsFiltered,
                )

                quotesStore.storeActual(values = updatedResponse.quotes)
            },
            onError = { error -> throw error },
        )
    }
        .onLeft {
            Timber.e(it)
            quotesStore.storeError(currenciesIds = params.currenciesIds)
        }
}