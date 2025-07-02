package com.tangem.data.quotes.multi

import arrow.core.Either
import com.tangem.data.common.api.safeApiCallWithTimeout
import com.tangem.data.quotes.store.QuotesStoreV2
import com.tangem.data.tokens.utils.QuotesUnsupportedCurrenciesIdAdapter
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.appcurrency.AppCurrencyResponseStore
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.quotes.multi.MultiQuoteFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [MultiQuoteFetcher]
 *
 * @property tangemTechApi            tangemTech api
 * @property appCurrencyResponseStore app currency response store
 * @property quotesStore              quotes store
 * @property dispatchers              dispatchers
 *
[REDACTED_AUTHOR]
 */
@Singleton
internal class DefaultMultiQuoteFetcher @Inject constructor(
    private val tangemTechApi: TangemTechApi,
    private val appCurrencyResponseStore: AppCurrencyResponseStore,
    private val quotesStore: QuotesStoreV2,
    private val dispatchers: CoroutineDispatcherProvider,
) : MultiQuoteFetcher {

    private val quotesUnsupportedCurrenciesAdapter = QuotesUnsupportedCurrenciesIdAdapter()

    override suspend fun invoke(params: MultiQuoteFetcher.Params) = Either.catchOn(dispatchers.default) {
        if (params.currenciesIds.isEmpty()) {
            Timber.d("No currencies to fetch quotes for")
            return@catchOn
        }

        quotesStore.refresh(currenciesIds = params.currenciesIds)

        val replacementIdsResult = quotesUnsupportedCurrenciesAdapter.replaceUnsupportedCurrencies(
            currenciesIds = params.currenciesIds.mapTo(
                destination = hashSetOf(),
                transform = CryptoCurrency.RawID::value,
            ),
        )

        val appCurrencyId = getAppCurrencyId(params = params)
        val coinIds = replacementIdsResult.idsForRequest.joinToString(separator = ",")

        val response = safeApiCallWithTimeout(
            call = {
                withContext(dispatchers.io) {
                    tangemTechApi.getQuotes(currencyId = appCurrencyId, coinIds = coinIds).bind()
                }
            },
            onError = { error -> throw error },
        )

        val updatedResponse = quotesUnsupportedCurrenciesAdapter.getResponseWithUnsupportedCurrencies(
            response = response,
            filteredIds = replacementIdsResult.idsFiltered,
        )

        quotesStore.storeActual(values = updatedResponse.quotes)
    }
        .onLeft {
            Timber.e(it)
            quotesStore.storeError(currenciesIds = params.currenciesIds)
        }

    private suspend fun getAppCurrencyId(params: MultiQuoteFetcher.Params): String {
        val appCurrencyId = params.appCurrencyId
            ?: appCurrencyResponseStore.getSyncOrNull()?.id

        if (appCurrencyId.isNullOrBlank()) {
            val exception = IllegalStateException("Unable to get AppCurrency for updating quotes")
            Timber.e(exception)

            throw exception
        }

        return appCurrencyId
    }
}