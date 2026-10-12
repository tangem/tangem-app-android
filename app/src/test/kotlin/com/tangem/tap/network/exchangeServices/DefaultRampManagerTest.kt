package com.tangem.tap.network.exchangeServices

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.express.ExpressServiceFetcher
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.offramp.repository.OfframpRepository
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultRampManagerTest {

    private val offrampRepository: OfframpRepository = mockk()
    private val expressServiceFetcher: ExpressServiceFetcher = mockk()
    private val currenciesRepository: CurrenciesRepository = mockk()

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()

    private val rampManager: RampStateManager = DefaultRampManager(
        offrampRepository = offrampRepository,
        expressServiceFetcher = expressServiceFetcher,
        currenciesRepository = currenciesRepository,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(offrampRepository, expressServiceFetcher, currenciesRepository)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetSellInitializationStatus {

        @Test
        fun `GIVEN offrampRepository status WHEN getSellInitializationStatus THEN returns the same flow`() = runTest {
            // Arrange
            val status = MutableStateFlow<Lce<Throwable, Any>>(Lce.Loading(partialContent = null))
            every { offrampRepository.initializationStatus } returns status

            // Act
            val actual = rampManager.getSellInitializationStatus()

            // Assert
            assertThat(actual).isSameInstanceAs(status)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FetchSellServiceData {

        @Test
        fun `GIVEN update succeeds WHEN fetchSellServiceData THEN offrampRepository is updated`() = runTest {
            // Arrange
            coEvery { offrampRepository.update() } returns Unit

            // Act
            rampManager.fetchSellServiceData()

            // Assert
            coVerify(exactly = 1) { offrampRepository.update() }
        }

        @Test
        fun `GIVEN update throws WHEN fetchSellServiceData THEN the exception is swallowed`() = runTest {
            // Arrange
            coEvery { offrampRepository.update() } throws IllegalStateException("boom")

            // Act & Assert (no exception escapes)
            rampManager.fetchSellServiceData()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AvailableForSell {

        private val userWalletId = UserWalletId("0011223344556677")

        private fun createStatus(
            amount: BigDecimal? = BigDecimal.ONE,
            yieldSupplyStatus: YieldSupplyStatus? = null,
        ): CryptoCurrencyStatus {
            val value: CryptoCurrencyStatus.Value = mockk(relaxed = true) {
                every { this@mockk.amount } returns amount
                every { this@mockk.yieldSupplyStatus } returns yieldSupplyStatus
            }
            return mockk {
                every { currency } returns cryptoCurrencyFactory.ethereum
                every { this@mockk.value } returns value
            }
        }

        @Test
        fun `GIVEN sell supported and nothing blocks sending WHEN availableForSell THEN returns Right`() = runTest {
            // Arrange
            every { offrampRepository.availableForSell(any()) } returns true
            coEvery { currenciesRepository.isSendBlockedByPendingTransactions(any(), any()) } returns false
            val status = createStatus()

            // Act
            val actual = rampManager.availableForSell(userWalletId, status, sendUnavailabilityReason = null)

            // Assert
            assertThat(actual.isRight()).isTrue()
        }

        @Test
        fun `GIVEN sell not supported by service WHEN availableForSell THEN returns Left NotSupportedBySellService`() =
            runTest {
                // Arrange
                every { offrampRepository.availableForSell(any()) } returns false
                coEvery { currenciesRepository.isSendBlockedByPendingTransactions(any(), any()) } returns false
                val status = createStatus()

                // Act
                val actual = rampManager.availableForSell(userWalletId, status, sendUnavailabilityReason = null)

                // Assert
                val expected = ScenarioUnavailabilityReason.NotSupportedBySellService(status.currency.name)
                assertThat(actual.leftOrNull()).isEqualTo(expected)
            }

        @Test
        fun `GIVEN offrampRepository availableForSell throws WHEN availableForSell THEN returns Left without checking send availability`() =
            runTest {
                // Arrange
                every { offrampRepository.availableForSell(any()) } throws IllegalStateException("boom")
                val status = createStatus()

                // Act
                val actual = rampManager.availableForSell(userWalletId, status, sendUnavailabilityReason = null)

                // Assert
                val expected = ScenarioUnavailabilityReason.NotSupportedBySellService(status.currency.name)
                assertThat(actual.leftOrNull()).isEqualTo(expected)
                coVerify(exactly = 0) { currenciesRepository.isSendBlockedByPendingTransactions(any(), any()) }
            }

        @Test
        fun `GIVEN explicit sendUnavailabilityReason WHEN availableForSell THEN uses it without consulting currenciesRepository`() =
            runTest {
                // Arrange
                every { offrampRepository.availableForSell(any()) } returns true
                val status = createStatus()

                // Act
                val actual = rampManager.availableForSell(
                    userWalletId,
                    status,
                    sendUnavailabilityReason = ScenarioUnavailabilityReason.JointAccount,
                )

                // Assert
                assertThat(actual.leftOrNull()).isEqualTo(ScenarioUnavailabilityReason.JointAccount)
                coVerify(exactly = 0) { currenciesRepository.isSendBlockedByPendingTransactions(any(), any()) }
            }

        @Test
        fun `GIVEN zero balance WHEN availableForSell THEN returns Left EmptyBalance with SELL scenario`() = runTest {
            // Arrange
            every { offrampRepository.availableForSell(any()) } returns true
            val status = createStatus(amount = BigDecimal.ZERO)

            // Act
            val actual = rampManager.availableForSell(userWalletId, status, sendUnavailabilityReason = null)

            // Assert
            val expected = ScenarioUnavailabilityReason.EmptyBalance(ScenarioUnavailabilityReason.WithdrawalScenario.SELL)
            assertThat(actual.leftOrNull()).isEqualTo(expected)
        }

        @Test
        fun `GIVEN pending transactions block sending WHEN availableForSell THEN returns Left PendingTransaction with SELL scenario`() =
            runTest {
                // Arrange
                every { offrampRepository.availableForSell(any()) } returns true
                coEvery { currenciesRepository.isSendBlockedByPendingTransactions(any(), any()) } returns true
                val status = createStatus()

                // Act
                val actual = rampManager.availableForSell(userWalletId, status, sendUnavailabilityReason = null)

                // Assert
                val expected = ScenarioUnavailabilityReason.PendingTransaction(
                    withdrawalScenario = ScenarioUnavailabilityReason.WithdrawalScenario.SELL,
                    networkName = status.currency.network.name,
                )
                assertThat(actual.leftOrNull()).isEqualTo(expected)
            }

        @Test
        fun `GIVEN yield supply blocks spending WHEN availableForSell THEN returns Left YieldSupplyApprovalRequired`() =
            runTest {
                // Arrange
                every { offrampRepository.availableForSell(any()) } returns true
                coEvery { currenciesRepository.isSendBlockedByPendingTransactions(any(), any()) } returns false
                val status = createStatus(
                    yieldSupplyStatus = YieldSupplyStatus(
                        isActive = true,
                        isInitialized = true,
                        isAllowedToSpend = false,
                        effectiveProtocolBalance = null,
                    ),
                )

                // Act
                val actual = rampManager.availableForSell(userWalletId, status, sendUnavailabilityReason = null)

                // Assert
                assertThat(actual.leftOrNull()).isEqualTo(ScenarioUnavailabilityReason.YieldSupplyApprovalRequired)
            }
    }
}