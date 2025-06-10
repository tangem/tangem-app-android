package com.tangem.data.quotes.single

import com.google.common.truth.Truth
import com.tangem.common.test.data.quote.MockQuoteResponseFactory
import com.tangem.data.quotes.multi.DefaultMultiQuoteStatusFetcher
import com.tangem.data.quotes.store.QuotesStatusesStore
import com.tangem.data.quotes.store.setSourceAsCache
import com.tangem.data.quotes.store.setSourceAsOnlyCache
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.CurrenciesResponse
import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.datasource.appcurrency.AppCurrencyResponseStore
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.quotes.single.SingleQuoteStatusFetcher
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class DefaultSingleQuoteStatusFetcherTest {

    private val tangemTechApi = mockk<TangemTechApi>(relaxed = true)
    private val appCurrencyResponseStore = mockk<AppCurrencyResponseStore>(relaxed = true)
    private val quotesStore = mockk<QuotesStatusesStore>(relaxed = true)

    private val multiFetcher = DefaultMultiQuoteStatusFetcher(
        tangemTechApi = tangemTechApi,
        appCurrencyResponseStore = appCurrencyResponseStore,
        quotesStatusesStore = quotesStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val singleFetcher = DefaultSingleQuoteStatusFetcher(multiFetcher)

    @Test
    fun `fetch single quote successfully`() = runTest {
        val params = SingleQuoteStatusFetcher.Params(rawCurrencyId = currenciesId, appCurrencyId = null)

        coEvery { appCurrencyResponseStore.getSyncOrNull() } returns usdAppCurrency

        val coinIds = "BTC"
        coEvery {
            tangemTechApi.getQuotes(currencyId = "usd", coinIds = coinIds)
        } returns ApiResponse.Success(successResponse)

        val actual = singleFetcher(params)

        coVerifyOrder {
            quotesStore.setSourceAsCache(currenciesIds = setOf(params.rawCurrencyId))
            appCurrencyResponseStore.getSyncOrNull()
            tangemTechApi.getQuotes(currencyId = "usd", coinIds = coinIds)
            quotesStore.store(values = successResponse.quotes)
        }

        coVerify(inverse = true) {
            quotesStore.setSourceAsOnlyCache(currenciesIds = any())
        }

        Truth.assertThat(actual.isRight()).isTrue()
    }

    @Test
    fun `fetch single quote successfully if appCurrencyId from params is not null`() = runTest {
        val appCurrencyId = "usd"
        val params = SingleQuoteStatusFetcher.Params(rawCurrencyId = currenciesId, appCurrencyId = appCurrencyId)

        val coinIds = "BTC"
        coEvery {
            tangemTechApi.getQuotes(currencyId = appCurrencyId, coinIds = coinIds)
        } returns ApiResponse.Success(successResponse)

        val actual = singleFetcher(params)

        coVerifyOrder {
            quotesStore.setSourceAsCache(currenciesIds = setOf(currenciesId))
            tangemTechApi.getQuotes(currencyId = "usd", coinIds = coinIds)
            quotesStore.store(values = successResponse.quotes)
        }

        Truth.assertThat(actual.isRight()).isTrue()
    }

    @Test
    fun `fetch single quote failure because appCurrencyId from params is blank`() = runTest {
        val appCurrencyId = ""
        val params = SingleQuoteStatusFetcher.Params(rawCurrencyId = currenciesId, appCurrencyId = appCurrencyId)

        val actual = singleFetcher(params)

        coVerifyOrder {
            quotesStore.setSourceAsCache(currenciesIds = setOf(currenciesId))
            quotesStore.setSourceAsOnlyCache(currenciesIds = setOf(currenciesId))
        }

        Truth.assertThat(actual.isLeft()).isTrue()
        Truth.assertThat(actual.leftOrNull()).isInstanceOf(IllegalStateException::class.java)
        Truth.assertThat(actual.leftOrNull()).hasMessageThat()
            .isEqualTo("Unable to get AppCurrency for updating quotes")
    }

    @Test
    fun `fetch single quote failure because api request failed`() = runTest {
        val params = SingleQuoteStatusFetcher.Params(rawCurrencyId = currenciesId, appCurrencyId = null)

        coEvery { appCurrencyResponseStore.getSyncOrNull() } returns usdAppCurrency

        val coinIds = "BTC"

        @Suppress("UNCHECKED_CAST")
        val errorResponse = ApiResponse.Error(ApiResponseError.NetworkException) as ApiResponse<QuotesResponse>
        coEvery { tangemTechApi.getQuotes(currencyId = "usd", coinIds = coinIds) } returns errorResponse

        val actual = singleFetcher(params)

        coVerifyOrder {
            quotesStore.setSourceAsCache(currenciesIds = setOf(currenciesId))
            appCurrencyResponseStore.getSyncOrNull()
            tangemTechApi.getQuotes(currencyId = "usd", coinIds = coinIds)
            quotesStore.setSourceAsOnlyCache(currenciesIds = setOf(currenciesId))
        }

        coVerify(inverse = true) {
            quotesStore.store(values = any())
        }

        Truth.assertThat(actual.isLeft()).isTrue()
    }

    @Test
    fun `fetch single quote failure because app currency not found`() = runTest {
        val params = SingleQuoteStatusFetcher.Params(rawCurrencyId = currenciesId, appCurrencyId = null)

        coEvery { appCurrencyResponseStore.getSyncOrNull() } returns null

        val actual = singleFetcher(params)

        coVerifyOrder {
            quotesStore.setSourceAsCache(currenciesIds = setOf(currenciesId))
            appCurrencyResponseStore.getSyncOrNull()
            quotesStore.setSourceAsOnlyCache(currenciesIds = setOf(currenciesId))
        }

        coVerify(inverse = true) {
            tangemTechApi.getQuotes(currencyId = any(), coinIds = any())
            quotesStore.store(values = any())
        }

        Truth.assertThat(actual.isLeft()).isTrue()
    }

    private companion object {

        val currenciesId = CryptoCurrency.RawID(value = "BTC")

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
            ),
        )
    }
}