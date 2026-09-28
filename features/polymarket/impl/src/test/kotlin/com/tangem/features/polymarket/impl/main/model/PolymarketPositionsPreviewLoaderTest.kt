package com.tangem.features.polymarket.impl.main.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.flow.DefaultPredictionPositionsUpdater
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketPositionsError
import com.tangem.domain.polymarket.model.PolymarketPositionsPage
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketPositionsPreviewUM
import com.tangem.features.polymarket.impl.positions.model.createPolymarketPosition
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketPositionsPreviewLoaderTest {

    private val repository: PolymarketPositionsRepository = mockk()
    private val deriveAddresses: DerivePolymarketAddressesUseCase = mockk()
    private val addresses: PolymarketAddresses = mockk()
    private val positionsUpdater = DefaultPredictionPositionsUpdater()
    private val userWalletId = UserWalletId("011")
    private val page = PolymarketPositionsPage(listOf(createPolymarketPosition()), hasNext = false)

    @Test
    fun `GIVEN open positions WHEN resumed THEN reads first three and preserves server order`() = runTest {
        // Arrange
        val loader = createLoader()
        val positions = listOf("c", "a", "b", "d").map { createPolymarketPosition(assetId = it) }
        coEvery { repository.getOpenPositions(addresses, 3, 0) } returns
            PolymarketPositionsPage(positions, hasNext = true).right()

        // Act
        loader.resume(backgroundScope, userWalletId)
        runCurrent()

        // Assert
        val content = loader.state.value as PolymarketPositionsPreviewUM.Content
        assertThat(
            content.positions.map { it.id },
        ).containsExactly("condition:c", "condition:a", "condition:b").inOrder()
        coVerify(exactly = 1) { repository.getOpenPositions(addresses, 3, 0) }
        coVerify(exactly = 0) { deriveAddresses.invoke(any()) }
    }

    @Test
    fun `GIVEN resumed preview WHEN poll interval passes THEN reads again`() = runTest {
        // Arrange
        val loader = createLoader()
        coEvery { repository.getOpenPositions(addresses, 3, 0) } returns page.right()
        loader.resume(backgroundScope, userWalletId)
        runCurrent()

        // Act
        passPollInterval()
        passPollInterval()

        // Assert
        coVerify(exactly = 3) { repository.getOpenPositions(addresses, 3, 0) }
    }

    @Test
    fun `GIVEN paused preview WHEN poll interval passes THEN nothing is requested until resumed`() = runTest {
        // Arrange
        val loader = createLoader()
        coEvery { repository.getOpenPositions(addresses, 3, 0) } returns page.right()
        loader.resume(backgroundScope, userWalletId)
        runCurrent()

        // Act
        loader.pause()
        advanceTimeBy(POLL_INTERVAL * 3)
        runCurrent()

        // Assert
        coVerify(exactly = 1) { repository.getOpenPositions(addresses, 3, 0) }

        // Act
        loader.resume(backgroundScope, userWalletId)
        runCurrent()

        // Assert
        coVerify(exactly = 2) { repository.getOpenPositions(addresses, 3, 0) }
    }

    @Test
    fun `GIVEN visible positions WHEN scheduled refresh fails THEN keeps cards outdated until next refresh succeeds`() =
        runTest {
            // Arrange
            val loader = createLoader()
            coEvery { repository.getOpenPositions(addresses, 3, 0) } returnsMany listOf(
                page.right(),
                PolymarketPositionsError.Network.left(),
                page.right(),
            )
            loader.resume(backgroundScope, userWalletId)
            runCurrent()
            val initial = loader.state.value as PolymarketPositionsPreviewUM.Content

            // Act
            passPollInterval()
            val outdated = loader.state.value as PolymarketPositionsPreviewUM.Content

            // Assert
            assertThat(outdated.positions).isEqualTo(initial.positions)
            assertThat(outdated.onRetry).isNotNull()

            // Act
            passPollInterval()

            // Assert
            assertThat(loader.state.value).isEqualTo(initial)
            coVerify(exactly = 3) { repository.getOpenPositions(addresses, 3, 0) }
        }

    @Test
    fun `GIVEN outdated preview WHEN retry tapped THEN reads at once and keeps polling`() = runTest {
        // Arrange
        val loader = createLoader()
        coEvery { repository.getOpenPositions(addresses, 3, 0) } returnsMany listOf(
            page.right(),
            PolymarketPositionsError.Network.left(),
            page.right(),
            page.right(),
        )
        loader.resume(backgroundScope, userWalletId)
        runCurrent()
        passPollInterval()
        val outdated = loader.state.value as PolymarketPositionsPreviewUM.Content

        // Act
        requireNotNull(outdated.onRetry).invoke()
        runCurrent()

        // Assert
        assertThat(loader.state.value).isEqualTo(PolymarketPositionsPreviewUM.Content(outdated.positions))
        coVerify(exactly = 3) { repository.getOpenPositions(addresses, 3, 0) }

        // Act
        passPollInterval()

        // Assert
        coVerify(exactly = 4) { repository.getOpenPositions(addresses, 3, 0) }
    }

    @Test
    fun `GIVEN a resumed preview WHEN the positions are invalidated THEN they are read again`() = runTest {
        // Arrange
        val loader = createLoader()
        coEvery { repository.getOpenPositions(addresses, 3, 0) } returns page.right()
        loader.resume(backgroundScope, userWalletId)
        runCurrent()

        // Act
        positionsUpdater.triggerUpdate(userWalletId)
        runCurrent()

        // Assert
        coVerify(exactly = 2) { repository.getOpenPositions(addresses, 3, 0) }
    }

    @Test
    fun `GIVEN the positions changed while paused WHEN the preview resumes THEN it is read once, not twice`() =
        runTest {
            // Arrange
            val loader = createLoader()
            coEvery { repository.getOpenPositions(addresses, 3, 0) } returns page.right()
            loader.resume(backgroundScope, userWalletId)
            runCurrent()
            loader.pause()

            // Act
            positionsUpdater.triggerUpdate(userWalletId)
            runCurrent()
            loader.resume(backgroundScope, userWalletId)
            runCurrent()

            // Assert
            coVerify(exactly = 2) { repository.getOpenPositions(addresses, 3, 0) }
        }

    @Test
    fun `GIVEN a paused preview WHEN the positions are invalidated THEN nothing is read`() = runTest {
        // Arrange
        val loader = createLoader()
        coEvery { repository.getOpenPositions(addresses, 3, 0) } returns page.right()
        loader.resume(backgroundScope, userWalletId)
        runCurrent()
        loader.pause()

        // Act
        positionsUpdater.triggerUpdate(userWalletId)
        runCurrent()

        // Assert
        coVerify(exactly = 1) { repository.getOpenPositions(addresses, 3, 0) }
    }

    @Test
    fun `GIVEN visible positions WHEN reload on resume fails THEN keeps cards until manual retry succeeds`() = runTest {
        // Arrange
        val loader = createLoader()
        coEvery { repository.getOpenPositions(addresses, 3, 0) } returnsMany listOf(
            page.right(),
            PolymarketPositionsError.Network.left(),
            page.right(),
        )
        loader.resume(backgroundScope, userWalletId)
        runCurrent()
        val initial = loader.state.value as PolymarketPositionsPreviewUM.Content

        // Act
        loader.pause()
        loader.resume(backgroundScope, userWalletId)
        runCurrent()
        val failed = loader.state.value as PolymarketPositionsPreviewUM.Content
        requireNotNull(failed.onRetry).invoke()
        runCurrent()

        // Assert
        assertThat(failed.positions).isEqualTo(initial.positions)
        assertThat(loader.state.value).isEqualTo(initial)
        coVerify(exactly = 3) { repository.getOpenPositions(addresses, 3, 0) }
    }

    @Test
    fun `GIVEN initial network failure WHEN retry succeeds empty THEN hides preview`() = runTest {
        // Arrange
        val loader = createLoader()
        coEvery { repository.getOpenPositions(addresses, 3, 0) } returnsMany listOf(
            PolymarketPositionsError.Network.left(),
            page.copy(positions = emptyList()).right(),
        )
        loader.resume(backgroundScope, userWalletId)
        runCurrent()
        val failed = loader.state.value as PolymarketPositionsPreviewUM.Error

        // Act
        failed.onRetry()
        runCurrent()

        // Assert
        assertThat(failed.isVisible).isTrue()
        assertThat(loader.state.value).isEqualTo(PolymarketPositionsPreviewUM.Content(persistentListOf()))
        assertThat(loader.state.value.isVisible).isFalse()
    }

    @Test
    fun `GIVEN unavailable stored addresses WHEN resumed THEN shows error without deriving keys`() = runTest {
        // Arrange
        val loader = createLoader()
        coEvery { deriveAddresses.stored(userWalletId) } returns null

        // Act
        loader.resume(backgroundScope, userWalletId)
        runCurrent()

        // Assert
        assertThat(loader.state.value).isInstanceOf(PolymarketPositionsPreviewUM.Error::class.java)
        coVerify(exactly = 0) { deriveAddresses.invoke(any()) }
        coVerify(exactly = 0) { repository.getOpenPositions(any(), any(), any()) }
    }

    @Test
    fun `GIVEN initial error WHEN paused and old retry invoked THEN does not request again`() = runTest {
        // Arrange
        val loader = createLoader()
        coEvery { repository.getOpenPositions(addresses, 3, 0) } returns PolymarketPositionsError.Network.left()
        loader.resume(backgroundScope, userWalletId)
        runCurrent()
        val failed = loader.state.value as PolymarketPositionsPreviewUM.Error

        // Act
        loader.pause()
        failed.onRetry()
        advanceTimeBy(POLL_INTERVAL * 3)
        runCurrent()

        // Assert
        assertThat(loader.state.value).isEqualTo(failed)
        coVerify(exactly = 1) { repository.getOpenPositions(addresses, 3, 0) }
    }

    @Test
    fun `GIVEN old request completes after resume WHEN new data arrived THEN ignores old result`() = runTest {
        // Arrange
        val loader = createLoader()
        val delayed = CompletableDeferred<Unit>()
        coEvery { repository.getOpenPositions(addresses, 3, 0) } coAnswers {
            withContext(NonCancellable) { delayed.await() }
            page.right()
        }
        loader.resume(backgroundScope, userWalletId)
        runCurrent()
        loader.pause()
        coEvery { repository.getOpenPositions(addresses, 3, 0) } returns page.copy(positions = emptyList()).right()

        // Act
        loader.resume(backgroundScope, userWalletId)
        runCurrent()
        delayed.complete(Unit)
        runCurrent()

        // Assert
        assertThat(loader.state.value).isEqualTo(PolymarketPositionsPreviewUM.Content(persistentListOf()))
    }

    @Test
    fun `GIVEN a read in flight WHEN the positions are invalidated THEN the stale response is dropped`() = runTest {
        // Arrange
        val loader = createLoader()
        val delayed = CompletableDeferred<Unit>()
        coEvery { repository.getOpenPositions(addresses, 3, 0) } coAnswers {
            withContext(NonCancellable) { delayed.await() }
            page.right()
        }
        loader.resume(backgroundScope, userWalletId)
        runCurrent()
        coEvery { repository.getOpenPositions(addresses, 3, 0) } returns page.copy(positions = emptyList()).right()

        // Act
        positionsUpdater.triggerUpdate(userWalletId)
        runCurrent()
        delayed.complete(Unit)
        runCurrent()

        // Assert
        assertThat(loader.state.value).isEqualTo(PolymarketPositionsPreviewUM.Content(persistentListOf()))
        coVerify(exactly = 2) { repository.getOpenPositions(addresses, 3, 0) }
    }

    @Nested
    inner class ManualRefresh {

        @Test
        fun `GIVEN visible positions WHEN manually refreshed THEN waits for the read and keeps polling`() = runTest {
            // Arrange
            val loader = createLoader()
            coEvery { repository.getOpenPositions(addresses, 3, 0) } returns page.right()
            loader.resume(backgroundScope, userWalletId)
            runCurrent()
            val initial = loader.state.value
            val read = CompletableDeferred<Unit>()
            coEvery { repository.getOpenPositions(addresses, 3, 0) } coAnswers {
                read.await()
                page.copy(positions = emptyList()).right()
            }

            // Act
            val refresh = backgroundScope.async { loader.refresh(backgroundScope, userWalletId) }
            runCurrent()

            // Assert
            assertThat(refresh.isCompleted).isFalse()
            assertThat(loader.state.value).isEqualTo(initial)

            // Act
            read.complete(Unit)
            runCurrent()

            // Assert
            assertThat(refresh.isCompleted).isTrue()
            assertThat(refresh.isCancelled).isFalse()
            assertThat(loader.state.value).isEqualTo(PolymarketPositionsPreviewUM.Content(persistentListOf()))

            // Act
            passPollInterval()

            // Assert
            coVerify(exactly = 3) { repository.getOpenPositions(addresses, 3, 0) }
        }

        @Test
        fun `GIVEN an older poll WHEN manual refresh finishes first THEN the old response is ignored`() = runTest {
            // Arrange
            val loader = createLoader()
            val oldRead = CompletableDeferred<Unit>()
            coEvery { repository.getOpenPositions(addresses, 3, 0) } coAnswers {
                withContext(NonCancellable) { oldRead.await() }
                page.right()
            }
            loader.resume(backgroundScope, userWalletId)
            runCurrent()
            coEvery { repository.getOpenPositions(addresses, 3, 0) } returns page.copy(positions = emptyList()).right()

            // Act
            val refresh = backgroundScope.async { loader.refresh(backgroundScope, userWalletId) }
            runCurrent()
            oldRead.complete(Unit)
            runCurrent()

            // Assert
            assertThat(refresh.isCompleted).isTrue()
            assertThat(loader.state.value).isEqualTo(PolymarketPositionsPreviewUM.Content(persistentListOf()))
        }

        @Test
        fun `GIVEN a loaded preview WHEN manual refresh fails THEN completes and preserves retryable cards`() = runTest {
            // Arrange
            val loader = createLoader()
            coEvery { repository.getOpenPositions(addresses, 3, 0) } returnsMany listOf(
                page.right(),
                PolymarketPositionsError.Network.left(),
            )
            loader.resume(backgroundScope, userWalletId)
            runCurrent()
            val initial = loader.state.value as PolymarketPositionsPreviewUM.Content

            // Act
            val refresh = backgroundScope.async { loader.refresh(backgroundScope, userWalletId) }
            runCurrent()

            // Assert
            assertThat(refresh.isCompleted).isTrue()
            assertThat(refresh.isCancelled).isFalse()
            val failed = loader.state.value as PolymarketPositionsPreviewUM.Content
            assertThat(failed.positions).isEqualTo(initial.positions)
            assertThat(failed.onRetry).isNotNull()
        }

        @Test
        fun `GIVEN an error then empty preview WHEN manually refreshed THEN can show the first position`() = runTest {
            // Arrange
            val loader = createLoader()
            coEvery { repository.getOpenPositions(addresses, 3, 0) } returnsMany listOf(
                PolymarketPositionsError.Network.left(),
                page.copy(positions = emptyList()).right(),
                page.right(),
            )
            loader.resume(backgroundScope, userWalletId)
            runCurrent()

            // Act
            backgroundScope.async { loader.refresh(backgroundScope, userWalletId) }
            runCurrent()

            // Assert
            assertThat(loader.state.value).isEqualTo(PolymarketPositionsPreviewUM.Content(persistentListOf()))

            // Act
            backgroundScope.async { loader.refresh(backgroundScope, userWalletId) }
            runCurrent()

            // Assert
            val content = loader.state.value as PolymarketPositionsPreviewUM.Content
            assertThat(content.positions.map { it.id }).containsExactly("condition:asset")
            coVerify(exactly = 3) { repository.getOpenPositions(addresses, 3, 0) }
        }

        @Test
        fun `GIVEN a manual refresh in flight WHEN paused THEN cancels the read and stops polling`() = runTest {
            // Arrange
            val loader = createLoader()
            coEvery { repository.getOpenPositions(addresses, 3, 0) } returns page.right()
            loader.resume(backgroundScope, userWalletId)
            runCurrent()
            val read = CompletableDeferred<Unit>()
            coEvery { repository.getOpenPositions(addresses, 3, 0) } coAnswers { read.await(); page.right() }
            val refresh = backgroundScope.async { loader.refresh(backgroundScope, userWalletId) }
            runCurrent()

            // Act
            loader.pause()
            passPollInterval()
            loader.refresh(backgroundScope, userWalletId)

            // Assert
            assertThat(refresh.isCancelled).isTrue()
            coVerify(exactly = 2) { repository.getOpenPositions(addresses, 3, 0) }
        }
    }

    @Nested
    inner class IndexingRetries {

        @Test
        fun `GIVEN positions invalidated WHEN indexing is delayed THEN reads with backoff before regular polling`() =
            runTest {
                // Arrange
                val loader = createLoader()
                val readTimes = mutableListOf<Long>()
                coEvery { repository.getOpenPositions(addresses, 3, 0) } coAnswers {
                    readTimes += testScheduler.currentTime
                    if (testScheduler.currentTime < 6_000) page.right()
                    else page.copy(positions = emptyList()).right()
                }
                loader.resume(backgroundScope, userWalletId)
                runCurrent()

                // Act
                positionsUpdater.triggerUpdate(userWalletId)
                runCurrent()
                advanceTimeBy(50.seconds)
                runCurrent()

                // Assert
                assertThat(readTimes).containsExactly(0L, 0L, 2_000L, 6_000L, 14_000L, 30_000L, 50_000L).inOrder()
                assertThat(loader.state.value).isEqualTo(PolymarketPositionsPreviewUM.Content(persistentListOf()))
            }

        @Test
        fun `GIVEN a trade while paused WHEN resumed THEN starts backoff and cancels it on pause`() = runTest {
            // Arrange
            val loader = createLoader()
            coEvery { repository.getOpenPositions(addresses, 3, 0) } returns page.right()
            loader.resume(backgroundScope, userWalletId)
            runCurrent()
            loader.pause()
            positionsUpdater.triggerUpdate(userWalletId)

            // Act
            loader.resume(backgroundScope, userWalletId)
            runCurrent()
            advanceTimeBy(2.seconds)
            runCurrent()
            loader.pause()
            advanceTimeBy(60.seconds)
            runCurrent()

            // Assert
            coVerify(exactly = 3) { repository.getOpenPositions(addresses, 3, 0) }
        }

        @Test
        fun `GIVEN another wallet trades WHEN polling THEN keeps the regular interval`() = runTest {
            // Arrange
            val loader = createLoader()
            coEvery { repository.getOpenPositions(addresses, 3, 0) } returns page.right()
            loader.resume(backgroundScope, userWalletId)
            runCurrent()

            // Act
            positionsUpdater.triggerUpdate(UserWalletId("012"))
            advanceTimeBy(POLL_INTERVAL - 1.seconds)
            runCurrent()

            // Assert
            coVerify(exactly = 1) { repository.getOpenPositions(addresses, 3, 0) }
        }
    }

    @Nested
    inner class PollingRestarts {

        @Test
        fun `GIVEN a pending read WHEN resumed repeatedly with eager dispatch THEN cancels before each new read`() =
            runTest {
                // Arrange
                val immediateDispatcher = UnconfinedTestDispatcher(testScheduler)
                val dispatchers = object : CoroutineDispatcherProvider by createTestingCoroutineDispatcherProvider() {
                    override val default = immediateDispatcher
                }
                val loader = createLoader(dispatchers)
                val scope = CoroutineScope(backgroundScope.coroutineContext + immediateDispatcher)
                val requests = mutableListOf<Job>()
                val previousRequestActive = mutableListOf<Boolean>()
                coEvery { repository.getOpenPositions(addresses, 3, 0) } coAnswers {
                    requests.lastOrNull()?.let { previousRequestActive += it.isActive }
                    requests += currentCoroutineContext().job
                    awaitCancellation()
                }

                // Act
                repeat(3) { loader.resume(scope, userWalletId) }

                // Assert
                assertThat(requests).hasSize(3)
                assertThat(previousRequestActive).containsExactly(false, false)

                // Act
                loader.pause()
                passPollInterval()

                // Assert
                assertThat(requests.all { it.isCancelled }).isTrue()
                coVerify(exactly = 3) { repository.getOpenPositions(addresses, 3, 0) }
            }

        @Test
        fun `GIVEN repeated resumes and manual refresh WHEN a trade arrives THEN keeps a single polling loop`() = runTest {
            // Arrange
            val loader = createLoader()
            val readTimes = mutableListOf<Long>()
            coEvery { repository.getOpenPositions(addresses, 3, 0) } coAnswers {
                readTimes += testScheduler.currentTime
                page.right()
            }
            repeat(3) {
                loader.resume(backgroundScope, userWalletId)
                runCurrent()
            }
            val refresh = backgroundScope.async { loader.refresh(backgroundScope, userWalletId) }
            runCurrent()
            refresh.await()

            // Act
            positionsUpdater.triggerUpdate(userWalletId)
            runCurrent()
            advanceTimeBy(50.seconds)
            runCurrent()
            loader.pause()
            advanceTimeBy(60.seconds)
            runCurrent()

            // Assert
            assertThat(readTimes).containsExactly(0L, 0L, 0L, 0L, 0L, 2_000L, 6_000L, 14_000L, 30_000L, 50_000L).inOrder()
        }
    }

    private fun TestScope.createLoader(
        dispatchers: CoroutineDispatcherProvider = createTestingCoroutineDispatcherProvider(),
    ): PolymarketPositionsPreviewLoader {
        coEvery { deriveAddresses.stored(userWalletId) } returns addresses
        return PolymarketPositionsPreviewLoader(
            repository = repository,
            deriveAddresses = deriveAddresses,
            updateListener = positionsUpdater,
            dispatchers = dispatchers,
        )
    }

    private fun TestScope.passPollInterval() {
        advanceTimeBy(POLL_INTERVAL)
        runCurrent()
    }

    private companion object {
        val POLL_INTERVAL = 20.seconds
    }
}