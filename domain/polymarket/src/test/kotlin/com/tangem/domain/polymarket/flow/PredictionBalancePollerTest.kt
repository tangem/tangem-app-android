package com.tangem.domain.polymarket.flow

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PredictionBalancePollingConfig
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val fetcher: PredictionAccountStatusFetcher = mockk()
    private val fundingWindow: PredictionFundingWindow = mockk()
    private val isOpen = MutableStateFlow(false)
    private val config = PredictionBalancePollingConfig(baseInterval = 30.seconds, fundingInterval = 5.seconds)

    @BeforeEach
    fun resetMocks() {
        clearMocks(fetcher, fundingWindow)
        coEvery { fetcher(any()) } returns Unit.right()
        isOpen.value = false
        every { fundingWindow.isOpen(any()) } returns isOpen
        coEvery { fundingWindow.watch(any()) } coAnswers { awaitCancellation() }
    }

    @Test
    fun `GIVEN subscriber WHEN subscribed THEN refreshes at once and then every base interval`() = runTest {
        // Arrange
        val poller = createPoller()

        // Act
        val subscription = poller.subscribe(WALLET_ID).launchIn(this)
        runCurrent()

        // Assert
        coVerify(exactly = 1) { fetcher(full(WALLET_ID)) }
        advanceTimeBy(30.seconds)
        runCurrent()
        coVerify(exactly = 2) { fetcher(full(WALLET_ID)) }
        advanceTimeBy(29.seconds)
        runCurrent()
        coVerify(exactly = 2) { fetcher(full(WALLET_ID)) }

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
        coVerify(exactly = 2) { fetcher(full(WALLET_ID)) }

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
        coVerify(exactly = 2) { fetcher(full(WALLET_ID)) }
    }

    @Test
    fun `GIVEN fetch hangs WHEN interval passes THEN next tick waits for it`() = runTest {
        // Arrange
        val gate = CompletableDeferred<Unit>()
        coEvery { fetcher(full(WALLET_ID)) } coAnswers { gate.await(); Unit.right() }
        val poller = createPoller()

        // Act
        val subscription = poller.subscribe(WALLET_ID).launchIn(this)
        advanceTimeBy(90.seconds)
        runCurrent()

        // Assert
        coVerify(exactly = 1) { fetcher(full(WALLET_ID)) }

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
        coVerify(exactly = 1) { fetcher(full(WALLET_ID)) }
        coVerify(exactly = 1) { fetcher(full(OTHER_WALLET_ID)) }

        first.cancel()
        second.cancel()
        runCurrent()
    }

    @Test
    fun `GIVEN loop died on a throwing fetch WHEN a new subscriber arrives THEN a fresh loop starts`() = runTest {
        // Arrange
        coEvery {
            fetcher(full(WALLET_ID))
        } throws IllegalStateException("boom") andThen Unit.right()
        val poller = PredictionBalancePoller(
            fetcher = fetcher,
            fundingWindow = fundingWindow,
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
        coVerify(exactly = 2) { fetcher(full(WALLET_ID)) }

        first.cancel()
        second.cancel()
        runCurrent()
    }

    @Test
    fun `GIVEN stale subscriber of a dead loop WHEN it cancels after a fresh loop started THEN the fresh loop keeps ticking`() =
        runTest {
            // Arrange
            coEvery {
                fetcher(full(WALLET_ID))
            } throws IllegalStateException("boom") andThen Unit.right()
            val poller = PredictionBalancePoller(
                fetcher = fetcher,
                fundingWindow = fundingWindow,
                config = config,
                appScope = TestAppCoroutineScope(
                    coroutineContext = StandardTestDispatcher(testScheduler) +
                        SupervisorJob() +
                        CoroutineExceptionHandler { _, _ -> },
                ),
            )

            // Act
            val stale = poller.subscribe(WALLET_ID).launchIn(this)
            runCurrent()
            val fresh = poller.subscribe(WALLET_ID).launchIn(this)
            runCurrent()

            // Assert
            coVerify(exactly = 2) { fetcher(full(WALLET_ID)) }

            stale.cancel()
            runCurrent()
            advanceTimeBy(30.seconds)
            runCurrent()
            coVerify(exactly = 3) { fetcher(full(WALLET_ID)) }

            fresh.cancel()
            runCurrent()
        }

    @Test
    fun `GIVEN fetch returns a Left WHEN interval passes THEN the loop keeps ticking`() = runTest {
        // Arrange
        coEvery {
            fetcher(full(WALLET_ID))
        } returns IllegalStateException("x").left()
        val poller = createPoller()

        // Act
        val subscription = poller.subscribe(WALLET_ID).launchIn(this)
        advanceTimeBy(30.seconds)
        runCurrent()

        // Assert
        coVerify(exactly = 2) { fetcher(full(WALLET_ID)) }

        subscription.cancel()
        runCurrent()
    }

    @Test
    fun `GIVEN window opens mid pause WHEN it opens THEN next refresh is at once and then every funding interval`() = runTest {
        // Arrange
        val poller = createPoller()
        val subscription = poller.subscribe(WALLET_ID).launchIn(this)
        advanceTimeBy(10.seconds)
        runCurrent()

        // Act
        isOpen.value = true
        runCurrent()

        // Assert
        coVerify(exactly = 1) { fetcher(full(WALLET_ID)) }
        coVerify(exactly = 1) { fetcher(availableOnly(WALLET_ID)) }
        advanceTimeBy(5.seconds)
        runCurrent()
        coVerify(exactly = 2) { fetcher(availableOnly(WALLET_ID)) }

        subscription.cancel()
        runCurrent()
    }

    @Test
    fun `GIVEN window closes WHEN next pause runs THEN base interval is back`() = runTest {
        // Arrange
        isOpen.value = true
        val poller = createPoller()
        val subscription = poller.subscribe(WALLET_ID).launchIn(this)
        runCurrent()

        // Act
        isOpen.value = false
        runCurrent()
        advanceTimeBy(29.seconds)
        runCurrent()

        // Assert
        coVerify(exactly = 1) { fetcher(availableOnly(WALLET_ID)) }
        coVerify(exactly = 1) { fetcher(full(WALLET_ID)) }
        advanceTimeBy(1.seconds)
        runCurrent()
        coVerify(exactly = 2) { fetcher(full(WALLET_ID)) }

        subscription.cancel()
        runCurrent()
    }

    @Test
    fun `GIVEN subscriber WHEN subscribed THEN window is watched until last unsubscribe`() = runTest {
        // Arrange
        var isWatching = false
        coEvery { fundingWindow.watch(WALLET_ID) } coAnswers {
            isWatching = true
            try {
                awaitCancellation()
            } finally {
                isWatching = false
            }
        }
        val poller = createPoller()

        // Act
        val subscription = poller.subscribe(WALLET_ID).launchIn(this)
        runCurrent()
        val whileSubscribed = isWatching
        subscription.cancel()
        runCurrent()

        // Assert
        assertThat(whileSubscribed).isTrue()
        assertThat(isWatching).isFalse()
    }

    private fun TestScope.createPoller() = PredictionBalancePoller(
        fetcher = fetcher,
        fundingWindow = fundingWindow,
        config = config,
        appScope = TestAppCoroutineScope(coroutineContext = StandardTestDispatcher(testScheduler)),
    )

    private fun full(userWalletId: UserWalletId) = PredictionAccountStatusFetcher.Params(
        userWalletId = userWalletId,
        mode = PredictionAccountStatusFetcher.Mode.FULL,
    )

    private fun availableOnly(userWalletId: UserWalletId) = PredictionAccountStatusFetcher.Params(
        userWalletId = userWalletId,
        mode = PredictionAccountStatusFetcher.Mode.AVAILABLE_ONLY,
    )

    private companion object {
        val WALLET_ID = UserWalletId("011")
        val OTHER_WALLET_ID = UserWalletId("022")
    }
}