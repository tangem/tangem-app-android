package com.tangem.domain.jointaccount.polling

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class JointAccountPollingCoordinatorTest {

    private val fetcher: SingleJointAccountListFetcher = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(fetcher)
        coEvery { fetcher.invoke(userWalletId = any()) } returns Unit.right()
    }

    @Test
    fun `GIVEN a wallet WHEN the first subscriber collects THEN no fetch happens until the poll interval elapses`() =
        runTest {
            // Arrange
            val coordinator = createCoordinator(testScope = this)
            val received = mutableListOf<Unit>()

            // Act
            backgroundScope.launch { coordinator.poll(walletId = WALLET_ID).collect { received += it } }
            runCurrent()

            // Assert — no fetch on subscribe, the ticker waits out its first interval like every one after it
            assertThat(received).isEmpty()
            coVerify(exactly = 0) { fetcher.invoke(userWalletId = WALLET_ID) }

            advanceTimeBy(POLL_INTERVAL_MILLIS)
            runCurrent()
            assertThat(received).hasSize(1)
            coVerify(exactly = 1) { fetcher.invoke(userWalletId = WALLET_ID) }
        }

    @Test
    fun `GIVEN two concurrent subscribers on the same wallet WHEN both collect THEN the fetch is not duplicated`() =
        runTest {
            // Arrange
            val coordinator = createCoordinator(testScope = this)
            val flow = coordinator.poll(walletId = WALLET_ID)
            val firstReceived = mutableListOf<Unit>()
            val secondReceived = mutableListOf<Unit>()

            // Act
            backgroundScope.launch { flow.collect { firstReceived += it } }
            backgroundScope.launch { flow.collect { secondReceived += it } }
            runCurrent()
            advanceTimeBy(POLL_INTERVAL_MILLIS)
            runCurrent()

            // Assert
            assertThat(firstReceived).hasSize(1)
            assertThat(secondReceived).hasSize(1)
            coVerify(exactly = 1) { fetcher.invoke(userWalletId = WALLET_ID) }
        }

    @Test
    fun `GIVEN a subscriber resubscribes within the grace period WHEN it collects THEN no extra fetch fires and the wait is not reset`() =
        runTest {
            // Arrange
            val coordinator = createCoordinator(testScope = this)
            val flow = coordinator.poll(walletId = WALLET_ID)

            // Act — the first subscriber waits out the interval for its fetch, then drops within the grace period
            val firstJob = backgroundScope.launch { flow.collect { } }
            runCurrent()
            advanceTimeBy(POLL_INTERVAL_MILLIS)
            runCurrent()
            coVerify(exactly = 1) { fetcher.invoke(userWalletId = WALLET_ID) }
            firstJob.cancel()

            advanceTimeBy(GRACE_PERIOD_MILLIS / 2)
            val secondReceived = mutableListOf<Unit>()
            backgroundScope.launch { flow.collect { secondReceived += it } }
            runCurrent()

            // Assert — resubscribing inside the grace period does not trigger another fetch
            coVerify(exactly = 1) { fetcher.invoke(userWalletId = WALLET_ID) }
            assertThat(secondReceived).isEmpty()

            // The remaining wait until the next tick elapses from the FIRST fetch, not from the resubscription
            advanceTimeBy(POLL_INTERVAL_MILLIS - GRACE_PERIOD_MILLIS / 2 - 1)
            runCurrent()
            coVerify(exactly = 1) { fetcher.invoke(userWalletId = WALLET_ID) }
            assertThat(secondReceived).isEmpty()

            advanceTimeBy(1)
            runCurrent()
            coVerify(exactly = 2) { fetcher.invoke(userWalletId = WALLET_ID) }
            assertThat(secondReceived).hasSize(1)
        }

    @Test
    fun `GIVEN a subscriber stays collecting WHEN time advances THEN a fetch fires every 60 seconds, starting at the first interval`() =
        runTest {
            // Arrange
            val coordinator = createCoordinator(testScope = this)
            val received = mutableListOf<Unit>()

            // Act & Assert — nothing before the first interval elapses
            backgroundScope.launch { coordinator.poll(walletId = WALLET_ID).collect { received += it } }
            runCurrent()
            coVerify(exactly = 0) { fetcher.invoke(userWalletId = WALLET_ID) }

            advanceTimeBy(POLL_INTERVAL_MILLIS - 1)
            runCurrent()
            coVerify(exactly = 0) { fetcher.invoke(userWalletId = WALLET_ID) }
            assertThat(received).isEmpty()

            advanceTimeBy(1)
            runCurrent()
            coVerify(exactly = 1) { fetcher.invoke(userWalletId = WALLET_ID) }
            assertThat(received).hasSize(1)

            advanceTimeBy(POLL_INTERVAL_MILLIS - 1)
            runCurrent()
            coVerify(exactly = 1) { fetcher.invoke(userWalletId = WALLET_ID) }
            assertThat(received).hasSize(1)

            advanceTimeBy(1)
            runCurrent()
            coVerify(exactly = 2) { fetcher.invoke(userWalletId = WALLET_ID) }
            assertThat(received).hasSize(2)
        }

    /**
     * A gap longer than the grace period tears down the ticker's producer coroutine — [lastFetchAt] is what
     * survives that and lets a restart wait out only what's left of the interval, so `now` must track the
     * same virtual clock `advanceTimeBy` drives, not the real wall clock.
     */
    @Test
    fun `GIVEN a subscriber leaves mid-interval and returns past the grace period WHEN it collects THEN only the remaining wait is left, not the full interval`() =
        runTest {
            // Arrange
            val coordinator = createCoordinator(testScope = this)
            val flow = coordinator.poll(walletId = WALLET_ID)

            // Act — the first fetch fires 60s in, then 10s later the subscriber drops and stays gone 1s past the
            // grace period
            val firstJob = backgroundScope.launch { flow.collect { } }
            runCurrent()
            advanceTimeBy(POLL_INTERVAL_MILLIS)
            runCurrent()
            coVerify(exactly = 1) { fetcher.invoke(userWalletId = WALLET_ID) }

            advanceTimeBy(TEN_SECONDS_MILLIS)
            firstJob.cancel()
            advanceTimeBy(GRACE_PERIOD_MILLIS + 1_000)

            val secondReceived = mutableListOf<Unit>()
            backgroundScope.launch { flow.collect { secondReceived += it } }
            runCurrent()

            // Assert — no immediate fetch: only 16s of the next 60s window have actually elapsed since the first fetch
            coVerify(exactly = 1) { fetcher.invoke(userWalletId = WALLET_ID) }
            assertThat(secondReceived).isEmpty()

            val alreadyElapsed = TEN_SECONDS_MILLIS + GRACE_PERIOD_MILLIS + 1_000
            advanceTimeBy(POLL_INTERVAL_MILLIS - alreadyElapsed - 1)
            runCurrent()
            coVerify(exactly = 1) { fetcher.invoke(userWalletId = WALLET_ID) }
            assertThat(secondReceived).isEmpty()

            advanceTimeBy(1)
            runCurrent()
            coVerify(exactly = 2) { fetcher.invoke(userWalletId = WALLET_ID) }
            assertThat(secondReceived).hasSize(1)
        }

    @Test
    fun `GIVEN a subscriber is away longer than the poll interval WHEN it returns THEN the fetch fires immediately`() =
        runTest {
            // Arrange
            val coordinator = createCoordinator(testScope = this)
            val flow = coordinator.poll(walletId = WALLET_ID)

            // Act
            val firstJob = backgroundScope.launch { flow.collect { } }
            runCurrent()
            advanceTimeBy(POLL_INTERVAL_MILLIS)
            runCurrent()
            coVerify(exactly = 1) { fetcher.invoke(userWalletId = WALLET_ID) }

            firstJob.cancel()
            advanceTimeBy(POLL_INTERVAL_MILLIS + TEN_SECONDS_MILLIS)

            val secondReceived = mutableListOf<Unit>()
            backgroundScope.launch { flow.collect { secondReceived += it } }
            runCurrent()

            // Assert — already overdue by the time it returns, so it fetches right away like a brand-new ticker
            coVerify(exactly = 2) { fetcher.invoke(userWalletId = WALLET_ID) }
            assertThat(secondReceived).hasSize(1)
        }

    @Test
    fun `GIVEN a fetch returns Left WHEN the next interval elapses THEN the ticker keeps polling`() = runTest {
        // Arrange
        coEvery { fetcher.invoke(userWalletId = WALLET_ID) } returns RuntimeException("network error").left()
        val coordinator = createCoordinator(testScope = this)
        val received = mutableListOf<Unit>()

        // Act
        backgroundScope.launch { coordinator.poll(walletId = WALLET_ID).collect { received += it } }
        runCurrent()
        advanceTimeBy(POLL_INTERVAL_MILLIS)
        runCurrent()

        // Assert — a failed fetch still emits and still paces the next one 60s out
        coVerify(exactly = 1) { fetcher.invoke(userWalletId = WALLET_ID) }
        assertThat(received).hasSize(1)

        advanceTimeBy(POLL_INTERVAL_MILLIS)
        runCurrent()
        coVerify(exactly = 2) { fetcher.invoke(userWalletId = WALLET_ID) }
        assertThat(received).hasSize(2)
    }

    /**
     * `fetch` itself never throws — a failed request surfaces as [arrow.core.Either.Left] — but the ticker must
     * still survive a genuine crash of its producer coroutine rather than leaving that wallet dead forever. The
     * restarted ticker has no [lastFetchAt] entry either (the crash pre-empted it), so it waits out its own
     * first interval on top of `retryWhen`'s own delay before it tries again.
     */
    @Test
    fun `GIVEN the producer coroutine crashes WHEN it retries after the interval THEN polling recovers`() = runTest {
        // Arrange
        coEvery { fetcher.invoke(userWalletId = WALLET_ID) } throws RuntimeException("boom") andThen Unit.right()
        val coordinator = createCoordinator(testScope = this)
        val received = mutableListOf<Unit>()

        // Act
        backgroundScope.launch { coordinator.poll(walletId = WALLET_ID).collect { received += it } }
        runCurrent()
        advanceTimeBy(POLL_INTERVAL_MILLIS)
        runCurrent()

        // Assert — the crash is swallowed by retryWhen, nothing emitted yet
        coVerify(exactly = 1) { fetcher.invoke(userWalletId = WALLET_ID) }
        assertThat(received).isEmpty()

        advanceTimeBy(POLL_INTERVAL_MILLIS * 2)
        runCurrent()
        coVerify(exactly = 2) { fetcher.invoke(userWalletId = WALLET_ID) }
        assertThat(received).hasSize(1)
    }

    @Test
    fun `GIVEN two different wallets WHEN each is polled on its own timeline THEN their tickers are independent`() =
        runTest {
            // Arrange
            val coordinator = createCoordinator(testScope = this)
            val firstReceived = mutableListOf<Unit>()

            // Act — wallet A starts first and gets its fetch 60s in
            backgroundScope.launch { coordinator.poll(walletId = WALLET_ID).collect { firstReceived += it } }
            runCurrent()
            advanceTimeBy(POLL_INTERVAL_MILLIS)
            runCurrent()
            coVerify(exactly = 1) { fetcher.invoke(userWalletId = WALLET_ID) }

            // 30s later, wallet B starts on its own clock
            advanceTimeBy(THIRTY_SECONDS_MILLIS)
            val secondReceived = mutableListOf<Unit>()
            backgroundScope.launch { coordinator.poll(walletId = OTHER_WALLET_ID).collect { secondReceived += it } }
            runCurrent()
            coVerify(exactly = 0) { fetcher.invoke(userWalletId = OTHER_WALLET_ID) }

            // Assert — wallet A is due 30s after this point (60s from its own fetch), wallet B needs a full 60s
            advanceTimeBy(THIRTY_SECONDS_MILLIS - 1)
            runCurrent()
            coVerify(exactly = 1) { fetcher.invoke(userWalletId = WALLET_ID) }
            coVerify(exactly = 0) { fetcher.invoke(userWalletId = OTHER_WALLET_ID) }

            advanceTimeBy(1)
            runCurrent()
            coVerify(exactly = 2) { fetcher.invoke(userWalletId = WALLET_ID) }
            coVerify(exactly = 0) { fetcher.invoke(userWalletId = OTHER_WALLET_ID) }
            assertThat(firstReceived).hasSize(2)
            assertThat(secondReceived).isEmpty()

            // Wallet B's own first fetch still needs another 30s (60s total from its own subscribe)
            advanceTimeBy(THIRTY_SECONDS_MILLIS)
            runCurrent()
            coVerify(exactly = 1) { fetcher.invoke(userWalletId = OTHER_WALLET_ID) }
            assertThat(secondReceived).hasSize(1)
        }

    private fun createCoordinator(testScope: TestScope): JointAccountPollingCoordinator {
        return JointAccountPollingCoordinator(
            fetcher = fetcher,
            appScope = TestAppCoroutineScope(testScope.backgroundScope.coroutineContext),
            now = { testScope.testScheduler.currentTime },
        )
    }

    private companion object {
        val WALLET_ID = UserWalletId("011")
        val OTHER_WALLET_ID = UserWalletId("012")
        const val POLL_INTERVAL_MILLIS = 60_000L
        const val GRACE_PERIOD_MILLIS = 5_000L
        const val TEN_SECONDS_MILLIS = 10_000L
        const val THIRTY_SECONDS_MILLIS = 30_000L
    }
}