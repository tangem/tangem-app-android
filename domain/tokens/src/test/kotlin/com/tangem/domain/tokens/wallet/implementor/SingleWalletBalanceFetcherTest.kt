package com.tangem.domain.tokens.wallet.implementor

import com.google.common.truth.Truth
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.wallet.FetchingSource
import com.tangem.domain.wallets.models.UserWalletId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SingleWalletBalanceFetcherTest {

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()

    private val currenciesRepository: CurrenciesRepository = mockk()
    private val fetcher = SingleWalletBalanceFetcher(currenciesRepository = currenciesRepository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(currenciesRepository)
    }

    @Test
    fun getFetchingSources() {
        // Act
        val actual = fetcher.fetchingSources

        // Assert
        val expected = setOf(FetchingSource.NETWORK, FetchingSource.QUOTE)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getCryptoCurrencies() = runTest {
        // Arrange
        val userWalletId = UserWalletId("011")
        val currency = cryptoCurrencyFactory.ethereum

        coEvery {
            currenciesRepository.getSingleCurrencyWalletPrimaryCurrency(userWalletId = userWalletId, refresh = true)
        } returns currency

        // Act
        val actual = fetcher.getCryptoCurrencies(userWalletId = userWalletId)

        // Assert
        val expected = setOf(currency)
        Truth.assertThat(actual).isEqualTo(expected)
    }
}