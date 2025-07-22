package com.tangem.data.quotes.single

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.quotes.single.SingleQuoteStatusFetcher
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultSingleQuoteStatusFetcherTest {

    private val multiFetcher = mockk<MultiQuoteStatusFetcher>()
    private val singleFetcher = DefaultSingleQuoteStatusFetcher(multiFetcher)

    @BeforeEach
    fun resetMocks() {
        clearMocks(multiFetcher)
    }

    @Test
    fun `fetch successfully if multiFetcher returns success`() = runTest {
        // Arrange
        val params = SingleQuoteStatusFetcher.Params(rawCurrencyId = currencyId, appCurrencyId = null)

        val multiFetcherParams = MultiQuoteStatusFetcher.Params(
            currenciesIds = setOf(params.rawCurrencyId),
            appCurrencyId = params.appCurrencyId,
        )

        coEvery { multiFetcher(multiFetcherParams) } returns Unit.right()

        // Act
        val actual = singleFetcher(params)

        // Assert
        val expected = Unit.right()
        Truth.assertThat(actual).isEqualTo(expected)

        coVerify(exactly = 1) { multiFetcher(multiFetcherParams) }
    }

    @Test
    fun `fetch failure if multiFetcher returns erro`() = runTest {
        // Arrange
        val params = SingleQuoteStatusFetcher.Params(rawCurrencyId = currencyId, appCurrencyId = null)

        val multiFetcherParams = MultiQuoteStatusFetcher.Params(
            currenciesIds = setOf(params.rawCurrencyId),
            appCurrencyId = params.appCurrencyId,
        )

        val multiFetcherError = IllegalStateException("").left()

        coEvery { multiFetcher(multiFetcherParams) } returns multiFetcherError

        // Act
        val actual = singleFetcher(params)

        // Assert
        val expected = multiFetcherError
        Truth.assertThat(actual).isEqualTo(expected)

        coVerify(exactly = 1) { multiFetcher(multiFetcherParams) }
    }

    private companion object {

        val currencyId = CryptoCurrency.RawID(value = "BTC")
    }
}