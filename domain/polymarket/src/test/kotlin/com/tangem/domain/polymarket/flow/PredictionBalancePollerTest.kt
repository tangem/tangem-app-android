package com.tangem.domain.polymarket.flow

import arrow.core.left
import arrow.core.right
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PredictionBalancePollingConfig
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

internal class PredictionBalancePollerTest {

    private val refresher: PredictionBalanceRefresher = mockk()
    private val config = PredictionBalancePollingConfig(baseInterval = 30.seconds)

    @BeforeEach
    fun resetMocks() {
        clearMocks(refresher)
        coEvery { refresher.refresh(any()) } returns Unit.right()
    }

    @Test
    fun `GIVEN subscriber WHEN subscribed THEN refreshes at once and then every base interval`() = runTest {
        // Arrange
        val poller = createPoller()

        // Act
        val subscription = poller.subscribe(WALLET_ID).launchIn(this)
        runCurrent()

        // Assert
        coVerify(exactly = 1) { refresher.refresh(WALLET_ID) }
        advanceTimeBy(30.seconds)
        runCurrent()
        coVerify(exactly = 2) { refresher.refresh(WALLET_ID) }
        advanceTimeBy(29.seconds)
        runCurrent()
        coVerify(exactly = 2) { refresher.refresh(WALLET_ID) }

        subscription.cancel()
        runCurrent()
    }

    @Test
    fun `GIVEN two subscribers WHEN both subscribed THEN one loop runs`() = runTest {
        // Arrange
        val poller = createPoller()

        // Act
        val first = poller.subscribe(WALLET_ID).launchIn(this)
        val second = poller.subscribe(WALLET_ID).launchIn(this)
        advanceTimeBy(30.seconds)
        runCurrent()

        // Assert
        coVerify(exactly = 2) { refresher.refresh(WALLET_ID) }

        first.cancel()
        second.cancel()
        runCurrent()
    }

    @Test
    fun `GIVEN last subscriber gone WHEN interval passes THEN no more refreshes`() = runTest {
        // Arrange
        val poller = createPoller()
        val first = poller.subscribe(WALLET_ID).launchIn(this)
        val second = poller.subscribe(WALLET_ID).launchIn(this)
        runCurrent()

        // Act
        first.cancel()
        advanceTimeBy(30.seconds)
        runCurrent()
        second.cancel()
        advanceTimeBy(60.seconds)
        runCurrent()

        // Assert
        coVerify(exactly = 2) { refresher.refresh(WALLET_ID) }
    }

    @Test
    fun `GIVEN refresh hangs WHEN interval passes THEN next tick waits for it`() = runTest {
        // Arrange
        val gate = CompletableDeferred<Unit>()
        coEvery { refresher.refresh(WALLET_ID) } coAnswers { gate.await(); Unit.right() }
        val poller = createPoller()

        // Act
        val subscription = poller.subscribe(WALLET_ID).launchIn(this)
        advanceTimeBy(90.seconds)
        runCurrent()

        // Assert
        coVerify(exactly = 1) { refresher.refresh(WALLET_ID) }

        gate.complete(Unit)
        subscription.cancel()
        runCurrent()
    }

    @Test
    fun `GIVEN two wallets WHEN both subscribed THEN each has its own loop`() = runTest {
        // Arrange
        val poller = createPoller()

        // Act
        val first = poller.subscribe(WALLET_ID).launchIn(this)
        val second = poller.subscribe(OTHER_WALLET_ID).launchIn(this)
        runCurrent()

        // Assert
        coVerify(exactly = 1) { refresher.refresh(WALLET_ID) }
        coVerify(exactly = 1) { refresher.refresh(OTHER_WALLET_ID) }

        first.cancel()
        second.cancel()
        runCurrent()
    }

    @Test
    fun `GIVEN loop died on a throwing refresh WHEN a new subscriber arrives THEN a fresh loop starts`() = runTest {
        // Arrange
        coEvery { refresher.refresh(WALLET_ID) } throws IllegalStateException("boom") andThen Unit.right()
        val poller = PredictionBalancePoller(
            refresher = refresher,
            config = config,
            appScope = TestAppCoroutineScope(
                coroutineContext = StandardTestDispatcher(testScheduler) +
                    SupervisorJob() +
                    CoroutineExceptionHandler { _, _ -> },
            ),
        )

        // Act
        val first = poller.subscribe(WALLET_ID).launchIn(this)
        runCurrent()
        val second = poller.subscribe(WALLET_ID).launchIn(this)
        runCurrent()

        // Assert
        coVerify(exactly = 2) { refresher.refresh(WALLET_ID) }

        first.cancel()
        second.cancel()
        runCurrent()
    }

    @Test
    fun `GIVEN refresh returns a Left WHEN interval passes THEN the loop keeps ticking`() = runTest {
        // Arrange
        coEvery { refresher.refresh(WALLET_ID) } returns IllegalStateException("x").left()
        val poller = createPoller()

        // Act
        val subscription = poller.subscribe(WALLET_ID).launchIn(this)
        advanceTimeBy(30.seconds)
        runCurrent()

        // Assert
        coVerify(exactly = 2) { refresher.refresh(WALLET_ID) }

        subscription.cancel()
        runCurrent()
    }

    private fun TestScope.createPoller() = PredictionBalancePoller(
        refresher = refresher,
        config = config,
        appScope = TestAppCoroutineScope(coroutineContext = StandardTestDispatcher(testScheduler)),
    )

    private companion object {
        val WALLET_ID = UserWalletId("011")
        val OTHER_WALLET_ID = UserWalletId("022")
    }
}