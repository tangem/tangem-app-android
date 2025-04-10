package com.tangem.data.quotes.multi

import com.google.common.truth.Truth
import com.tangem.common.test.data.quote.MockQuoteResponseFactory
import com.tangem.data.quotes.store.QuotesStoreV2
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.CurrenciesResponse
import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.datasource.appcurrency.AppCurrencyResponseStore
import com.tangem.domain.quotes.multi.MultiQuoteFetcher
import com.tangem.domain.tokens.model.CryptoCurrency
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
internal class DefaultMultiQuoteFetcherTest {

    private val tangemTechApi = mockk<TangemTechApi>(relaxed = true)
    private val appCurrencyResponseStore = mockk<AppCurrencyResponseStore>(relaxed = true)
    private val quotesStore = mockk<QuotesStoreV2>(relaxed = true)

    private val fetcher = DefaultMultiQuoteFetcher(
        tangemTechApi = tangemTechApi,
        appCurrencyResponseStore = appCurrencyResponseStore,
        quotesStore = quotesStore,
    )

    @Test
    fun `fetch quotes successfully`() = runTest {
        val params = MultiQuoteFetcher.Params(currenciesIds = currenciesIds)

        coEvery { appCurrencyResponseStore.getSyncOrNull() } returns usdAppCurrency

        val coinIds = "BTC,ETH"
        coEvery {
            tangemTechApi.getQuotes(currencyId = "usd", coinIds = coinIds)
        } returns ApiResponse.Success(successResponse)

        val actual = fetcher(params)

        coVerifyOrder {
            quotesStore.refresh(currenciesIds = params.currenciesIds)

            appCurrencyResponseStore.getSyncOrNull()

            tangemTechApi.getQuotes(currencyId = "usd", coinIds = coinIds)

            quotesStore.storeActual(values = successResponse.quotes)
        }

        coVerify(inverse = true) {
            quotesStore.storeError(currenciesIds = any())
        }

        Truth.assertThat(actual.isRight()).isTrue()
    }

    @Test
    fun `fetch quotes failure because api request failed`() = runTest {
        val params = MultiQuoteFetcher.Params(currenciesIds = currenciesIds)

        coEvery { appCurrencyResponseStore.getSyncOrNull() } returns usdAppCurrency

        val coinIds = "BTC,ETH"

        @Suppress("UNCHECKED_CAST")
        val errorResponse = ApiResponse.Error(ApiResponseError.NetworkException) as ApiResponse<QuotesResponse>
        coEvery { tangemTechApi.getQuotes(currencyId = "usd", coinIds = coinIds) } returns errorResponse

        val actual = fetcher(params)

        coVerifyOrder {
            quotesStore.refresh(currenciesIds = params.currenciesIds)

            appCurrencyResponseStore.getSyncOrNull()

            tangemTechApi.getQuotes(currencyId = "usd", coinIds = coinIds)

            quotesStore.storeError(currenciesIds = params.currenciesIds)
        }

        coVerify(inverse = true) {
            quotesStore.storeActual(values = any())
        }

        Truth.assertThat(actual.isLeft()).isTrue()
    }

    @Test
    fun `fetch quotes failure because app currency not found`() = runTest {
        val params = MultiQuoteFetcher.Params(currenciesIds = currenciesIds)

        coEvery { appCurrencyResponseStore.getSyncOrNull() } returns null

        val actual = fetcher(params)

        coVerifyOrder {
            quotesStore.refresh(currenciesIds = params.currenciesIds)
            appCurrencyResponseStore.getSyncOrNull()
            quotesStore.storeError(currenciesIds = params.currenciesIds)
        }

        coVerify(inverse = true) {
            tangemTechApi.getQuotes(currencyId = any(), coinIds = any())
            quotesStore.storeActual(values = any())
        }

        Truth.assertThat(actual.isLeft()).isTrue()
    }

    private companion object {

        val currenciesIds = setOf(
            CryptoCurrency.RawID(value = "BTC"),
            CryptoCurrency.RawID(value = "ETH"),
        )

        val usdAppCurrency = CurrenciesResponse.Currency(
            id = "USD".lowercase(),
            code = "USD",
            name = "US Dollar",
            unit = "$",
            type = "fiat",
            rateBTC = "",
        )

        val successResponse = QuotesResponse(
            quotes = mapOf(
                "BTC" to MockQuoteResponseFactory.createSinglePrice(value = BigDecimal.ONE),
                "ETH" to MockQuoteResponseFactory.createSinglePrice(value = BigDecimal.TEN),
            ),
        )
    }
}