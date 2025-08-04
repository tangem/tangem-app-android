package com.tangem.domain.tokens.wallet.implementor

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesFetcher
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.tokens.wallet.FetchingSource
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MultiWalletBalanceFetcherTest {

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()

    private val multiWalletFetcher: MultiWalletCryptoCurrenciesFetcher = mockk()
    private val multiWalletSupplier: MultiWalletCryptoCurrenciesSupplier = mockk()
    private val fetcher = MultiWalletBalanceFetcher(
        multiWalletCryptoCurrenciesFetcher = multiWalletFetcher,
        multiWalletCryptoCurrenciesSupplier = multiWalletSupplier,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(multiWalletFetcher)
    }

    @Test
    fun getFetchingSources() {
        // Act
        val actual = fetcher.fetchingSources

        // Assert
        val expected = setOf(FetchingSource.NETWORK, FetchingSource.QUOTE, FetchingSource.STAKING)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `getCryptoCurrencies successfully if multiWalletCryptoCurrenciesFetcher RETURNS SUCCESS`() = runTest {
        // Arrange
        val currencies: Set<CryptoCurrency> = cryptoCurrencyFactory.ethereumAndStellar.toSet()
        val supplierFlow = flowOf(currencies)

        coEvery {
            multiWalletFetcher(params = MultiWalletCryptoCurrenciesFetcher.Params(userWalletId = userWalletId))
        } returns Unit.right()

        every {
            multiWalletSupplier(params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId = userWalletId))
        } returns supplierFlow

        // Act
        val actual = fetcher.getCryptoCurrencies(userWalletId = userWalletId)

        // Assert
        val expected = currencies.toSet()
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `getCryptoCurrencies successfully if multiWalletCryptoCurrenciesFetcher RETURNS ERROR`() = runTest {
        // Arrange
        val currencies: Set<CryptoCurrency> = cryptoCurrencyFactory.ethereumAndStellar.toSet()
        val supplierFlow = flowOf(currencies)

        coEvery {
            multiWalletFetcher(params = MultiWalletCryptoCurrenciesFetcher.Params(userWalletId = userWalletId))
        } returns IllegalStateException().left()

        every {
            multiWalletSupplier(params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId = userWalletId))
        } returns supplierFlow

        // Act
        val actual = fetcher.getCryptoCurrencies(userWalletId = userWalletId)

        // Assert
        val expected = currencies.toSet()
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `getCryptoCurrencies successfully if multiWalletCryptoCurrenciesSupplier IS EMPTY FLOW`() = runTest {
        // Arrange
        val supplierFlow = emptyFlow<Set<CryptoCurrency>>()

        coEvery {
            multiWalletFetcher(params = MultiWalletCryptoCurrenciesFetcher.Params(userWalletId = userWalletId))
        } returns Unit.right()

        every {
            multiWalletSupplier(params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId = userWalletId))
        } returns supplierFlow

        // Act
        val actual = fetcher.getCryptoCurrencies(userWalletId = userWalletId)

        // Assert
        val expected = emptySet<CryptoCurrency>()
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private companion object {
        val userWalletId = UserWalletId("011")
    }
}