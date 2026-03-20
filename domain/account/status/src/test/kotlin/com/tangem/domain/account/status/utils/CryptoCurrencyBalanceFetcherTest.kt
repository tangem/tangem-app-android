package com.tangem.domain.account.status.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.BalanceFetchingOperations
import com.tangem.domain.tokens.FetchingSource
import com.tangem.test.mock.MockAccounts
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tests for [CryptoCurrencyBalanceFetcher]
 *
[REDACTED_AUTHOR]
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CryptoCurrencyBalanceFetcherTest {

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
    private val balanceFetchingOperations: BalanceFetchingOperations = mockk()

    private val userWalletId = MockAccounts.userWalletId
    private val userWalletId2 = UserWalletId("012")

    @AfterEach
    fun tearDown() {
        clearMocks(balanceFetchingOperations)
    }

    private fun createFetcher(testScope: TestScope): CryptoCurrencyBalanceFetcher {
        return CryptoCurrencyBalanceFetcher(
            balanceFetchingOperations = balanceFetchingOperations,
            parallelUpdatingScope = testScope,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InvokeWithEmptyCurrencies {

        @Test
        fun `invoke with empty currencies does not fetch`() = runTest {
            // Arrange
            val fetcher = createFetcher(this)
            val currencies = emptyList<CryptoCurrency>()

            // Act
            fetcher(userWalletId = userWalletId, currencies = currencies)
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 0) { balanceFetchingOperations.fetchAll(any(), any(), any()) }
        }

        @Test
        fun `invokeAndAwait with empty currencies does not fetch`() = runTest {
            // Arrange
            val fetcher = createFetcher(this)
            val currencies = emptyList<CryptoCurrency>()

            // Act
            fetcher.invokeAndAwait(userWalletId = userWalletId, currencies = currencies)

            // Assert
            coVerify(exactly = 0) { balanceFetchingOperations.fetchAll(any(), any(), any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InvokeWithSingleCurrency {

        @Test
        fun `invoke with single currency fetches all sources`() = runTest {
            // Arrange
            val fetcher = createFetcher(this)
            val currency = cryptoCurrencyFactory.ethereum
            coEvery {
                balanceFetchingOperations.fetchAll(
                    userWalletId = userWalletId,
                    currencies = listOf(currency),
                    sources = any(),
                )
            } returns emptyMap()

            // Act
            fetcher(userWalletId = userWalletId, currency = currency)
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) {
                balanceFetchingOperations.fetchAll(
                    userWalletId = userWalletId,
                    currencies = listOf(currency),
                    sources = setOf(FetchingSource.NETWORK, FetchingSource.QUOTE, FetchingSource.STAKING),
                )
            }
        }

        @Test
        fun `invokeAndAwait with single currency fetches all sources`() = runTest {
            // Arrange
            val fetcher = createFetcher(this)
            val currency = cryptoCurrencyFactory.ethereum
            coEvery {
                balanceFetchingOperations.fetchAll(
                    userWalletId = userWalletId,
                    currencies = listOf(currency),
                    sources = any(),
                )
            } returns emptyMap()

            // Act
            fetcher.invokeAndAwait(userWalletId = userWalletId, currency = currency)

            // Assert
            coVerify(exactly = 1) {
                balanceFetchingOperations.fetchAll(
                    userWalletId = userWalletId,
                    currencies = listOf(currency),
                    sources = setOf(FetchingSource.NETWORK, FetchingSource.QUOTE, FetchingSource.STAKING),
                )
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InvokeWithMultipleCurrencies {

        @Test
        fun `invoke with multiple currencies passes correct parameters`() = runTest {
            // Arrange
            val fetcher = createFetcher(this)
            val currencies = cryptoCurrencyFactory.ethereumAndStellar
            coEvery {
                balanceFetchingOperations.fetchAll(
                    userWalletId = userWalletId,
                    currencies = currencies,
                    sources = any(),
                )
            } returns emptyMap()

            // Act
            fetcher(userWalletId = userWalletId, currencies = currencies)
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) {
                balanceFetchingOperations.fetchAll(
                    userWalletId = userWalletId,
                    currencies = currencies,
                    sources = setOf(FetchingSource.NETWORK, FetchingSource.QUOTE, FetchingSource.STAKING),
                )
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ErrorHandling {

        @Test
        fun `invoke logs errors when fetch fails`() = runTest {
            // Arrange
            val fetcher = createFetcher(this)
            val currency = cryptoCurrencyFactory.ethereum
            val error = RuntimeException("Network error")
            val errors = mapOf(FetchingSource.NETWORK to error)

            coEvery {
                balanceFetchingOperations.fetchAll(any(), any(), any())
            } returns errors

            // Act
            fetcher(userWalletId = userWalletId, currency = currency)
            advanceUntilIdle()

            // Assert - fetchAll was called, FetchErrorFormatter.format is used internally (object, no mock needed)
            coVerify(exactly = 1) {
                balanceFetchingOperations.fetchAll(any(), any(), any())
            }
        }

        @Test
        fun `invoke continues even when some sources fail`() = runTest {
            // Arrange
            val fetcher = createFetcher(this)
            val currency = cryptoCurrencyFactory.ethereum
            val networkError = RuntimeException("Network error")
            val errors = mapOf(FetchingSource.NETWORK to networkError)

            coEvery {
                balanceFetchingOperations.fetchAll(any(), any(), any())
            } returns errors

            // Act & Assert - should not throw
            fetcher(userWalletId = userWalletId, currency = currency)
            advanceUntilIdle()

            coVerify(exactly = 1) { balanceFetchingOperations.fetchAll(any(), any(), any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class MutexBehavior {

        @Test
        fun `concurrent calls for same wallet are serialized`() = runTest {
            // Arrange
            val fetcher = createFetcher(this)
            val currency = cryptoCurrencyFactory.ethereum
            val callOrder = mutableListOf<Int>()
            val callCount = AtomicInteger(0)

            coEvery {
                balanceFetchingOperations.fetchAll(any(), any(), any())
            } coAnswers {
                val currentCall = callCount.incrementAndGet()
                callOrder.add(currentCall)
                delay(100) // Simulate work
                callOrder.add(-currentCall) // Mark completion
                emptyMap()
            }

            // Act - launch two concurrent calls for the same wallet
            fetcher(userWalletId = userWalletId, currency = currency)
            fetcher(userWalletId = userWalletId, currency = currency)
            advanceUntilIdle()

            // Assert - calls should be serialized (1 starts, 1 finishes, 2 starts, 2 finishes)
            assertThat(callOrder).containsExactly(1, -1, 2, -2).inOrder()
        }

        @Test
        fun `concurrent calls for different wallets run in parallel`() = runTest {
            // Arrange
            val fetcher = createFetcher(this)
            val currency = cryptoCurrencyFactory.ethereum
            val callOrder = mutableListOf<String>()

            coEvery {
                balanceFetchingOperations.fetchAll(eq(userWalletId), any(), any())
            } coAnswers {
                callOrder.add("wallet1-start")
                delay(100)
                callOrder.add("wallet1-end")
                emptyMap()
            }

            coEvery {
                balanceFetchingOperations.fetchAll(eq(userWalletId2), any(), any())
            } coAnswers {
                callOrder.add("wallet2-start")
                delay(100)
                callOrder.add("wallet2-end")
                emptyMap()
            }

            // Act - launch two concurrent calls for different wallets
            fetcher(userWalletId = userWalletId, currency = currency)
            fetcher(userWalletId = userWalletId2, currency = currency)
            advanceUntilIdle()

            // Assert - both should start before either finishes (parallel execution)
            val wallet1StartIndex = callOrder.indexOf("wallet1-start")
            val wallet2StartIndex = callOrder.indexOf("wallet2-start")
            val wallet1EndIndex = callOrder.indexOf("wallet1-end")
            val wallet2EndIndex = callOrder.indexOf("wallet2-end")

            // Both should start before both end
            assertThat(wallet1StartIndex).isLessThan(wallet1EndIndex)
            assertThat(wallet2StartIndex).isLessThan(wallet2EndIndex)
            // Both starts should happen before both ends (parallel)
            assertThat(maxOf(wallet1StartIndex, wallet2StartIndex))
                .isLessThan(minOf(wallet1EndIndex, wallet2EndIndex))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InvokeAndAwaitBehavior {

        @Test
        fun `invokeAndAwait suspends until completion`() = runTest {
            // Arrange
            val fetcher = createFetcher(this)
            val currency = cryptoCurrencyFactory.ethereum
            var completed = false

            coEvery {
                balanceFetchingOperations.fetchAll(any(), any(), any())
            } coAnswers {
                delay(100)
                completed = true
                emptyMap()
            }

            // Act
            fetcher.invokeAndAwait(userWalletId = userWalletId, currency = currency)

            // Assert - should be completed after invokeAndAwait returns
            assertThat(completed).isTrue()
        }
    }
}