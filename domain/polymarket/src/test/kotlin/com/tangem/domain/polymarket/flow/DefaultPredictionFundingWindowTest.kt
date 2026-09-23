package com.tangem.domain.polymarket.flow

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PredictionBalancePollingConfig
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal class DefaultPredictionFundingWindowTest {

    private val statusSupplier: PredictionAccountStatusSupplier = mockk()

    private val status = MutableStateFlow<PredictionAccountStatusValue>(createActive(BigDecimal("1.00")))
    private var now = 1_000_000L

    @BeforeEach
    fun resetMocks() {
        clearMocks(statusSupplier)
        status.value = createActive(BigDecimal("1.00"))
        now = 1_000_000L
        every { statusSupplier(WALLET_ID) } returns status
    }

    @Test
    fun `GIVEN a closed window WHEN a top-up is sent THEN the window opens`() = runTest {
        // Arrange
        val window = createWindow()
        val watching = launch { window.watch(WALLET_ID) }
        window.isOpen(WALLET_ID).test {
            assertThat(awaitItem()).isFalse()

            // Act
            window.open(WALLET_ID)

            // Assert
            assertThat(awaitItem()).isTrue()
        }
        watching.cancel()
    }

    @Test
    fun `GIVEN an open window WHEN the balance changes THEN the window closes`() = runTest {
        // Arrange
        val window = createWindow()
        val watching = launch { window.watch(WALLET_ID) }
        window.isOpen(WALLET_ID).test {
            assertThat(awaitItem()).isFalse()
            window.open(WALLET_ID)
            assertThat(awaitItem()).isTrue()

            // Act
            status.value = createActive(BigDecimal("3.00"))

            // Assert
            assertThat(awaitItem()).isFalse()
        }
        watching.cancel()
    }

    @Test
    fun `GIVEN an open window WHEN the balance is restated unchanged THEN the window stays open`() = runTest {
        // Arrange
        val window = createWindow()
        val watching = launch { window.watch(WALLET_ID) }
        window.isOpen(WALLET_ID).test {
            assertThat(awaitItem()).isFalse()
            window.open(WALLET_ID)
            assertThat(awaitItem()).isTrue()

            // Act
            status.value = createActive(BigDecimal("1.000"))
            runCurrent()

            // Assert
            expectNoEvents()
        }
        watching.cancel()
    }

    @Test
    fun `GIVEN an open window WHEN the balance never changes THEN the window closes on the deadline`() = runTest {
        // Arrange
        val window = createWindow()
        val watching = launch { window.watch(WALLET_ID) }
        window.isOpen(WALLET_ID).test {
            assertThat(awaitItem()).isFalse()
            window.open(WALLET_ID)
            assertThat(awaitItem()).isTrue()

            // Act
            advanceTimeBy(2.minutes.inWholeMilliseconds + 1)

            // Assert
            assertThat(awaitItem()).isFalse()
        }
        watching.cancel()
    }

    @Test
    fun `GIVEN the balance is unknown WHEN the window is open THEN it still closes on the deadline`() = runTest {
        // Arrange
        val window = createWindow()
        status.value = PredictionAccountStatusValue.Loading
        val watching = launch { window.watch(WALLET_ID) }
        window.isOpen(WALLET_ID).test {
            assertThat(awaitItem()).isFalse()
            window.open(WALLET_ID)
            assertThat(awaitItem()).isTrue()

            // Act
            advanceTimeBy(2.minutes.inWholeMilliseconds + 1)

            // Assert
            assertThat(awaitItem()).isFalse()
        }
        watching.cancel()
    }

    @Test
    fun `GIVEN a window opened while nobody watched WHEN watching starts late THEN the full window is still left`() =
        runTest {
            // Arrange
            val window = createWindow()
            window.open(WALLET_ID)
            now += 5.minutes.inWholeMilliseconds

            // Act
            val watching = launch { window.watch(WALLET_ID) }
            runCurrent()
            window.isOpen(WALLET_ID).test {
                assertThat(awaitItem()).isTrue()
                advanceTimeBy(2.minutes.inWholeMilliseconds - 1)
                expectNoEvents()
                advanceTimeBy(2)

                // Assert
                assertThat(awaitItem()).isFalse()
            }
            watching.cancel()
        }

    @Test
    fun `GIVEN a top-up sent longer ago than the max age WHEN watching starts THEN the window never opens`() = runTest {
        // Arrange
        val window = createWindow()
        window.open(WALLET_ID)
        now += 10.minutes.inWholeMilliseconds + 1

        // Act
        val watching = launch { window.watch(WALLET_ID) }
        runCurrent()

        // Assert
        window.isOpen(WALLET_ID).test {
            assertThat(awaitItem()).isFalse()
            expectNoEvents()
        }
        watching.cancel()
    }

    @Test
    fun `GIVEN funds landed before anybody watched WHEN watching starts THEN the window closes at once`() = runTest {
        // Arrange
        val window = createWindow()
        window.open(WALLET_ID)
        runCurrent()
        status.value = createActive(BigDecimal("3.00"))

        // Act
        val watching = launch { window.watch(WALLET_ID) }
        runCurrent()

        // Assert
        window.isOpen(WALLET_ID).test {
            assertThat(awaitItem()).isFalse()
            expectNoEvents()
        }
        watching.cancel()
    }

    @Test
    fun `GIVEN the balance was unknown at open WHEN the first known balance changes THEN the window closes`() =
        runTest {
            // Arrange
            val window = createWindow()
            status.value = PredictionAccountStatusValue.Loading
            window.open(WALLET_ID)
            val watching = launch { window.watch(WALLET_ID) }
            runCurrent()
            window.isOpen(WALLET_ID).test {
                assertThat(awaitItem()).isTrue()
                status.value = createActive(BigDecimal("1.00"))
                runCurrent()
                expectNoEvents()

                // Act
                status.value = createActive(BigDecimal("3.00"))

                // Assert
                assertThat(awaitItem()).isFalse()
            }
            watching.cancel()
        }

    @Test
    fun `GIVEN the clock moved back since open WHEN watching starts THEN the window lasts no longer than configured`() =
        runTest {
            // Arrange
            val window = createWindow()
            window.open(WALLET_ID)
            now -= 20.seconds.inWholeMilliseconds

            // Act
            val watching = launch { window.watch(WALLET_ID) }
            runCurrent()
            window.isOpen(WALLET_ID).test {
                assertThat(awaitItem()).isTrue()
                advanceTimeBy(2.minutes.inWholeMilliseconds + 1)

                // Assert
                assertThat(awaitItem()).isFalse()
            }
            watching.cancel()
        }

    @Test
    fun `GIVEN the max age is nearer than the window WHEN watching starts THEN the max age wins`() = runTest {
        // Arrange
        val window = createWindow()
        window.open(WALLET_ID)
        now += 9.minutes.inWholeMilliseconds

        // Act
        val watching = launch { window.watch(WALLET_ID) }
        runCurrent()
        window.isOpen(WALLET_ID).test {
            assertThat(awaitItem()).isTrue()
            advanceTimeBy(1.minutes.inWholeMilliseconds + 1)

            // Assert
            assertThat(awaitItem()).isFalse()
        }
        watching.cancel()
    }

    @Test
    fun `GIVEN an open window WHEN the screen is left and re-entered THEN the deadline is not restarted`() = runTest {
        // Arrange
        val window = createWindow()
        window.open(WALLET_ID)
        val firstWatch = launch { window.watch(WALLET_ID) }
        runCurrent()
        advanceTimeBy(90.seconds.inWholeMilliseconds)
        firstWatch.cancel()
        now += 90.seconds.inWholeMilliseconds

        // Act
        val secondWatch = launch { window.watch(WALLET_ID) }
        window.isOpen(WALLET_ID).test {
            assertThat(awaitItem()).isTrue()
            advanceTimeBy(30.seconds.inWholeMilliseconds + 1)

            // Assert
            assertThat(awaitItem()).isFalse()
        }
        secondWatch.cancel()
    }

    @Test
    fun `GIVEN an open window WHEN the balance changes while the screen is left THEN it closes on re-entry`() =
        runTest {
            // Arrange
            val window = createWindow()
            window.open(WALLET_ID)
            val firstWatch = launch { window.watch(WALLET_ID) }
            runCurrent()
            firstWatch.cancel()
            status.value = createActive(BigDecimal("4.00"))

            // Act
            val secondWatch = launch { window.watch(WALLET_ID) }
            runCurrent()

            // Assert
            window.isOpen(WALLET_ID).test {
                assertThat(awaitItem()).isFalse()
                expectNoEvents()
            }
            secondWatch.cancel()
        }

    @Test
    fun `GIVEN a window opened for one wallet WHEN another wallet is observed THEN it stays closed`() = runTest {
        // Arrange
        val window = createWindow()
        val watching = launch { window.watch(WALLET_ID) }

        // Act
        window.open(WALLET_ID)

        // Assert
        window.isOpen(OTHER_WALLET_ID).test {
            assertThat(awaitItem()).isFalse()
            expectNoEvents()
        }
        watching.cancel()
    }

    private fun TestScope.createWindow() = DefaultPredictionFundingWindow(
        predictionAccountStatusSupplier = statusSupplier,
        config = PredictionBalancePollingConfig(fundingWindow = 2.minutes, fundingWindowMaxAge = 10.minutes),
        appScope = TestAppCoroutineScope(coroutineContext = StandardTestDispatcher(testScheduler)),
        currentTimeMillis = { now },
    )

    private fun createActive(balance: BigDecimal) = PredictionAccountStatusValue.Active(
        source = StatusSource.ACTUAL,
        balance = balance,
        positionsValue = null,
        fiatRate = null,
        depositCurrency = COLLATERAL,
        depositAddress = null,
    )

    private companion object {
        val WALLET_ID = UserWalletId("011")
        val OTHER_WALLET_ID = UserWalletId("022")
        val COLLATERAL: CryptoCurrency.Token = mockk()
    }
}