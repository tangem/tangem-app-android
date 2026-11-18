package com.tangem.domain.jointaccount.polling

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.jointaccount.fetcher.SingleJointAccountListFetcher
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import io.mockk.every
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class JointAccountPollingCoordinatorTest {

    private val fetcher: SingleJointAccountListFetcher = mockk()
    private val isLinked = MutableStateFlow(true)
    private val userWallets = MutableStateFlow<List<UserWallet>?>(
        listOf(WALLET_ID, OTHER_WALLET_ID).map { id -> mockk<UserWallet> { every { walletId } returns id } },
    )
    private val userWalletsListRepository: UserWalletsListRepository = mockk {
        every { userWallets } returns this@JointAccountPollingCoordinatorTest.userWallets
    }

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
    fun `GIVEN a subscriber stays collecting WHEN time advances THEN a fetch fires every poll interval, starting at the first interval`() =
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

            // Act — the first fetch fires one interval in, then the subscriber drops shortly after and stays gone
            // 1s past the grace period; the ticker keeps running during the grace period and fetches once more
            val firstJob = backgroundScope.launch { flow.collect { } }
            runCurrent()
            advanceTimeBy(POLL_INTERVAL_MILLIS)
            runCurrent()
            coVerify(exactly = 1) { fetcher.invoke(userWalletId = WALLET_ID) }

            advanceTimeBy(LEAVE_OFFSET_MILLIS)
            firstJob.cancel()
            advanceTimeBy(GRACE_PERIOD_MILLIS + 1_000)

            val secondReceived = mutableListOf<Unit>()
            backgroundScope.launch { flow.collect { secondReceived += it } }
            runCurrent()

            // Assert — no immediate fetch: the last one (inside the grace period) was less than an interval ago
            coVerify(exactly = 2) { fetcher.invoke(userWalletId = WALLET_ID) }
            assertThat(secondReceived).isEmpty()

            val lastFetchAt = POLL_INTERVAL_MILLIS * 2
            val returnedAt = POLL_INTERVAL_MILLIS + LEAVE_OFFSET_MILLIS + GRACE_PERIOD_MILLIS + 1_000
            val alreadyElapsed = returnedAt - lastFetchAt
            advanceTimeBy(POLL_INTERVAL_MILLIS - alreadyElapsed - 1)
            runCurrent()
            coVerify(exactly = 2) { fetcher.invoke(userWalletId = WALLET_ID) }
            assertThat(secondReceived).isEmpty()

            advanceTimeBy(1)
            runCurrent()
            coVerify(exactly = 3) { fetcher.invoke(userWalletId = WALLET_ID) }
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

            // The ticker keeps running during the grace period and fetches once more before it is torn down
            advanceTimeBy(LEAVE_OFFSET_MILLIS)
            firstJob.cancel()
            advanceTimeBy(GRACE_PERIOD_MILLIS + POLL_INTERVAL_MILLIS)
            coVerify(exactly = 2) { fetcher.invoke(userWalletId = WALLET_ID) }

            val secondReceived = mutableListOf<Unit>()
            backgroundScope.launch { flow.collect { secondReceived += it } }
            runCurrent()

            // Assert — already overdue by the time it returns, so it fetches right away like a brand-new ticker
            coVerify(exactly = 3) { fetcher.invoke(userWalletId = WALLET_ID) }
            assertThat(secondReceived).hasSize(1)
        }

    @Test
    fun `GIVEN a wallet never polled WHEN polled with an immediate first fetch THEN it fetches at once and then paces itself`() =
        runTest {
            // Arrange
            val coordinator = createCoordinator(testScope = this)

            // Act
            backgroundScope.launch { coordinator.poll(walletId = WALLET_ID, isFirstFetchImmediate = true).collect { } }
            runCurrent()

            // Assert
            coVerify(exactly = 1) { fetcher.invoke(userWalletId = WALLET_ID) }

            advanceTimeBy(POLL_INTERVAL_MILLIS - 1)
            runCurrent()
            coVerify(exactly = 1) { fetcher.invoke(userWalletId = WALLET_ID) }

            advanceTimeBy(1)
            runCurrent()
            coVerify(exactly = 2) { fetcher.invoke(userWalletId = WALLET_ID) }
        }

    @Test
    fun `GIVEN a wallet fetched a moment ago WHEN polled with an immediate first fetch THEN it waits out the interval`() =
        runTest {
            // Arrange
            val coordinator = createCoordinator(testScope = this)
            val firstJob = backgroundScope.launch { coordinator.poll(walletId = WALLET_ID).collect { } }
            runCurrent()
            advanceTimeBy(POLL_INTERVAL_MILLIS)
            runCurrent()
            firstJob.cancel()
            runCurrent()

            // Act
            backgroundScope.launch { coordinator.poll(walletId = WALLET_ID, isFirstFetchImmediate = true).collect { } }
            runCurrent()

            // Assert
            coVerify(exactly = 1) { fetcher.invoke(userWalletId = WALLET_ID) }
        }

    @Test
    fun `GIVEN every fetch fails WHEN time advances THEN the wait doubles up to a minute`() = runTest {
        // Arrange
        coEvery { fetcher.invoke(userWalletId = WALLET_ID) } returns RuntimeException("503").left()
        val coordinator = createCoordinator(testScope = this)
        val fetchTimes = mutableListOf<Long>()

        // Act
        backgroundScope.launch { coordinator.poll(walletId = WALLET_ID).collect { fetchTimes += currentTime } }
        advanceTimeBy(delayTimeMillis = 300_000L)
        runCurrent()

        // Assert
        val waits = (listOf(0L) + fetchTimes).zipWithNext { previous, next -> next - previous }
        assertThat(waits.take(n = 7))
            .containsExactly(5_000L, 10_000L, 20_000L, 40_000L, 60_000L, 60_000L, 60_000L)
            .inOrder()
    }

    @Test
    fun `GIVEN fetches failed in a row WHEN one succeeds THEN the next one is back at the poll interval`() = runTest {
        // Arrange
        coEvery { fetcher.invoke(userWalletId = WALLET_ID) } returnsMany listOf(
            RuntimeException("503").left(),
            RuntimeException("503").left(),
            Unit.right(),
        )
        val coordinator = createCoordinator(testScope = this)
        val fetchTimes = mutableListOf<Long>()

        // Act
        backgroundScope.launch { coordinator.poll(walletId = WALLET_ID).collect { fetchTimes += currentTime } }
        advanceTimeBy(delayTimeMillis = 45_000L)
        runCurrent()

        // Assert
        val waits = (listOf(0L) + fetchTimes).zipWithNext { previous, next -> next - previous }
        assertThat(waits).containsExactly(5_000L, 10_000L, 20_000L, 5_000L, 5_000L).inOrder()
    }

    @Test
    fun `GIVEN fetches failed in a row WHEN every subscriber leaves and one returns THEN the pace starts over`() =
        runTest {
            // Arrange
            coEvery { fetcher.invoke(userWalletId = WALLET_ID) } returns RuntimeException("503").left()
            val coordinator = createCoordinator(testScope = this)
            val firstJob = backgroundScope.launch { coordinator.poll(walletId = WALLET_ID).collect { } }
            advanceTimeBy(delayTimeMillis = 35_000L)
            runCurrent()
            firstJob.cancel()
            advanceTimeBy(GRACE_PERIOD_MILLIS + 1)
            runCurrent()
            clearMocks(fetcher, answers = false)
            val returnedAt = currentTime
            val fetchTimes = mutableListOf<Long>()

            // Act
            backgroundScope.launch { coordinator.poll(walletId = WALLET_ID).collect { fetchTimes += currentTime } }
            advanceTimeBy(delayTimeMillis = 16_000L)
            runCurrent()

            // Assert — asked at once, and the failure doubles the wait from the base interval, not from 40 seconds
            assertThat(fetchTimes.map { it - returnedAt }).containsExactly(0L, 10_000L).inOrder()
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

            // Act — wallet A starts first and gets its fetch one interval in
            backgroundScope.launch { coordinator.poll(walletId = WALLET_ID).collect { firstReceived += it } }
            runCurrent()
            advanceTimeBy(POLL_INTERVAL_MILLIS)
            runCurrent()
            coVerify(exactly = 1) { fetcher.invoke(userWalletId = WALLET_ID) }

            // Half an interval later, wallet B starts on its own clock
            advanceTimeBy(HALF_INTERVAL_MILLIS)
            val secondReceived = mutableListOf<Unit>()
            backgroundScope.launch { coordinator.poll(walletId = OTHER_WALLET_ID).collect { secondReceived += it } }
            runCurrent()
            coVerify(exactly = 0) { fetcher.invoke(userWalletId = OTHER_WALLET_ID) }

            // Assert — wallet A is due half an interval after this point, wallet B needs a full interval
            advanceTimeBy(HALF_INTERVAL_MILLIS - 1)
            runCurrent()
            coVerify(exactly = 1) { fetcher.invoke(userWalletId = WALLET_ID) }
            coVerify(exactly = 0) { fetcher.invoke(userWalletId = OTHER_WALLET_ID) }

            advanceTimeBy(1)
            runCurrent()
            coVerify(exactly = 2) { fetcher.invoke(userWalletId = WALLET_ID) }
            coVerify(exactly = 0) { fetcher.invoke(userWalletId = OTHER_WALLET_ID) }
            assertThat(firstReceived).hasSize(2)
            assertThat(secondReceived).isEmpty()

            // Wallet B's own first fetch still needs another half interval (a full one from its own subscribe)
            advanceTimeBy(HALF_INTERVAL_MILLIS)
            runCurrent()
            coVerify(exactly = 1) { fetcher.invoke(userWalletId = OTHER_WALLET_ID) }
            assertThat(secondReceived).hasSize(1)
        }

    @Test
    fun `GIVEN a wallet not linked to the device WHEN intervals elapse THEN nothing is fetched`() = runTest {
        // Arrange
        isLinked.value = false
        val coordinator = createCoordinator(testScope = this)
        val received = mutableListOf<Unit>()

        // Act
        backgroundScope.launch { coordinator.poll(walletId = WALLET_ID).collect { received += it } }
        runCurrent()
        advanceTimeBy(POLL_INTERVAL_MILLIS * 3)
        runCurrent()

        // Assert
        coVerify(exactly = 0) { fetcher.invoke(userWalletId = any()) }
        assertThat(received).isEmpty()
    }

    @Test
    fun `GIVEN a wallet not linked WHEN it gets linked THEN polling starts`() = runTest {
        // Arrange
        isLinked.value = false
        val coordinator = createCoordinator(testScope = this)
        backgroundScope.launch { coordinator.poll(walletId = WALLET_ID).collect() }
        runCurrent()
        advanceTimeBy(POLL_INTERVAL_MILLIS * 2)

        // Act
        isLinked.value = true
        runCurrent()
        advanceTimeBy(POLL_INTERVAL_MILLIS)
        runCurrent()

        // Assert
        coVerify(exactly = 1) { fetcher.invoke(userWalletId = WALLET_ID) }
    }

    @Test
    fun `GIVEN a polled wallet WHEN its link is lost THEN polling stops`() = runTest {
        // Arrange
        val coordinator = createCoordinator(testScope = this)
        backgroundScope.launch { coordinator.poll(walletId = WALLET_ID).collect() }
        runCurrent()
        advanceTimeBy(POLL_INTERVAL_MILLIS)
        runCurrent()

        // Act
        isLinked.value = false
        runCurrent()
        advanceTimeBy(POLL_INTERVAL_MILLIS * 3)
        runCurrent()

        // Assert
        coVerify(exactly = 1) { fetcher.invoke(userWalletId = WALLET_ID) }
    }

    @Test
    fun `GIVEN a polled wallet WHEN it is forgotten THEN polling stops and the request in flight is cancelled`() =
        runTest {
            // Arrange
            var isRequestCancelled = false
            coEvery { fetcher.invoke(userWalletId = WALLET_ID) } coAnswers {
                try {
                    awaitCancellation()
                } finally {
                    isRequestCancelled = true
                }
            }
            val coordinator = createCoordinator(testScope = this)
            backgroundScope.launch { coordinator.poll(walletId = WALLET_ID).collect { } }
            advanceTimeBy(POLL_INTERVAL_MILLIS)
            runCurrent()

            // Act
            userWallets.value = userWallets.value.orEmpty().filterNot { it.walletId == WALLET_ID }
            runCurrent()
            advanceTimeBy(delayTimeMillis = 60_000L)
            runCurrent()

            // Assert
            assertThat(isRequestCancelled).isTrue()
            coVerify(exactly = 1) { fetcher.invoke(userWalletId = WALLET_ID) }
        }

    private fun createCoordinator(testScope: TestScope): JointAccountPollingCoordinator {
        return JointAccountPollingCoordinator(
            fetcher = fetcher,
            isWalletLinked = { isLinked },
            userWalletsListRepository = userWalletsListRepository,
            appScope = TestAppCoroutineScope(testScope.backgroundScope.coroutineContext),
            now = { testScope.testScheduler.currentTime },
        )
    }

    private companion object {
        val WALLET_ID = UserWalletId("011")
        val OTHER_WALLET_ID = UserWalletId("012")
        const val POLL_INTERVAL_MILLIS = 5_000L
        const val GRACE_PERIOD_MILLIS = 5_000L
        const val HALF_INTERVAL_MILLIS = POLL_INTERVAL_MILLIS / 2
        const val LEAVE_OFFSET_MILLIS = 1_000L
    }
}