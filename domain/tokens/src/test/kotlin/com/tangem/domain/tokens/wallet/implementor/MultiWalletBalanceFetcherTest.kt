package com.tangem.domain.tokens.wallet.implementor

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.MultiWalletAccountListFetcher
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.tokens.FetchingSource
import com.tangem.domain.tokens.wallet.WalletFetchingSource
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

    private val multiWalletFetcher: MultiWalletAccountListFetcher = mockk()
    private val multiWalletSupplier: MultiWalletCryptoCurrenciesSupplier = mockk()
    private val fetcher = MultiWalletBalanceFetcher(
        multiWalletAccountListFetcher = multiWalletFetcher,
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
        val expected = setOf(
            WalletFetchingSource.Balance(
                sources = setOf(FetchingSource.NETWORK, FetchingSource.QUOTE, FetchingSource.STAKING),
            ),
            WalletFetchingSource.TangemPay,
        )
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `getCryptoCurrencies successfully if multiWalletCryptoCurrenciesFetcher RETURNS SUCCESS`() = runTest {
        // Arrange
        val currencies: Set<CryptoCurrency> = cryptoCurrencyFactory.ethereumAndStellar.toSet()
        val supplierFlow = flowOf(currencies)

        coEvery {
            multiWalletFetcher(params = MultiWalletAccountListFetcher.Params(userWalletId = userWalletId))
        } returns Unit.right()

        every {
            multiWalletSupplier(params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId = userWalletId))
        } returns supplierFlow

        // Act
        val actual = fetcher.getCryptoCurrencies(userWallet = userWallet)

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
            multiWalletFetcher(params = MultiWalletAccountListFetcher.Params(userWalletId = userWalletId))
        } returns IllegalStateException().left()

        every {
            multiWalletSupplier(params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId = userWalletId))
        } returns supplierFlow

        // Act
        val actual = fetcher.getCryptoCurrencies(userWallet = userWallet)

        // Assert
        val expected = currencies.toSet()
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `getCryptoCurrencies successfully if multiWalletCryptoCurrenciesSupplier IS EMPTY FLOW`() = runTest {
        // Arrange
        val supplierFlow = emptyFlow<Set<CryptoCurrency>>()

        coEvery {
            multiWalletFetcher(params = MultiWalletAccountListFetcher.Params(userWalletId = userWalletId))
        } returns Unit.right()

        every {
            multiWalletSupplier(params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId = userWalletId))
        } returns supplierFlow

        // Act
        val actual = fetcher.getCryptoCurrencies(userWallet = userWallet)

        // Assert
        val expected = emptySet<CryptoCurrency>()
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private companion object {
        val userWalletId = UserWalletId("011")
        val userWallet: UserWallet = mockk {
            every { walletId } returns userWalletId
        }
    }
}