package com.tangem.domain.tokens

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.balance.BalanceContributionFetcher
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BalanceFetchingOperationsTest {

    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher = mockk()
    private val multiQuoteStatusFetcher: MultiQuoteStatusFetcher = mockk()
    private val firstContributionFetcher: BalanceContributionFetcher = mockk()
    private val secondContributionFetcher: BalanceContributionFetcher = mockk()

    private val operations = BalanceFetchingOperations(
        multiNetworkStatusFetcher = multiNetworkStatusFetcher,
        multiQuoteStatusFetcher = multiQuoteStatusFetcher,
        contributionFetchers = setOf(firstContributionFetcher, secondContributionFetcher),
    )

    private val userWalletId = UserWalletId("011")
    private val ethereum = MockCryptoCurrencyFactory().ethereum
    private val currencies = listOf(ethereum)

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            multiNetworkStatusFetcher,
            multiQuoteStatusFetcher,
            firstContributionFetcher,
            secondContributionFetcher,
        )
        coEvery { multiNetworkStatusFetcher(any()) } returns Either.Right(Unit)
        coEvery { multiQuoteStatusFetcher(any()) } returns Either.Right(Unit)
        coEvery { firstContributionFetcher.fetch(any(), any()) } returns Either.Right(Unit)
        coEvery { secondContributionFetcher.fetch(any(), any()) } returns Either.Right(Unit)
    }

    @Test
    fun `GIVEN contributions requested WHEN fetchAll THEN every contribution fetcher is called`() = runTest {
        // Act
        val actual = operations.fetchAll(userWalletId, currencies, setOf(FetchingSource.CONTRIBUTIONS))

        // Assert
        assertThat(actual).isEmpty()
        coVerify(exactly = 1) {
            firstContributionFetcher.fetch(userWalletId, currencies)
            secondContributionFetcher.fetch(userWalletId, currencies)
        }
        coVerify(exactly = 0) {
            multiNetworkStatusFetcher(any())
            multiQuoteStatusFetcher(any())
        }
    }

    @Test
    fun `GIVEN contributions not requested WHEN fetchAll THEN no contribution fetcher is called`() = runTest {
        // Act
        operations.fetchAll(userWalletId, currencies, setOf(FetchingSource.NETWORK, FetchingSource.QUOTE))

        // Assert
        coVerify(exactly = 1) {
            multiNetworkStatusFetcher(any())
            multiQuoteStatusFetcher(any())
        }
        coVerify(exactly = 0) {
            firstContributionFetcher.fetch(any(), any())
            secondContributionFetcher.fetch(any(), any())
        }
    }

    @Test
    fun `GIVEN one contribution and network fail WHEN fetchAll THEN errors keyed by their names`() = runTest {
        // Arrange
        val contributionError = IllegalStateException("contribution")
        val networkError = IllegalStateException("network")
        coEvery { firstContributionFetcher.fetch(any(), any()) } returns Either.Left(contributionError)
        coEvery { multiNetworkStatusFetcher(any()) } returns Either.Left(networkError)

        // Act
        val actual = operations.fetchAll(
            userWalletId = userWalletId,
            currencies = currencies,
            sources = setOf(FetchingSource.NETWORK, FetchingSource.QUOTE, FetchingSource.CONTRIBUTIONS),
        )

        // Assert
        assertThat(actual).containsExactly(
            FetchingSource.NETWORK.name, networkError,
            firstContributionFetcher.javaClass.simpleName, contributionError,
        )
        coVerify(exactly = 1) { secondContributionFetcher.fetch(userWalletId, currencies) }
    }
}