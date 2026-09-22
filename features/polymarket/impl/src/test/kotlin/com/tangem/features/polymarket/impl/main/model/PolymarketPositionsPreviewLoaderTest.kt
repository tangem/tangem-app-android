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
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
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

    private fun TestScope.createLoader(): PolymarketPositionsPreviewLoader {
        coEvery { deriveAddresses.stored(userWalletId) } returns addresses
        val dispatcher = StandardTestDispatcher(testScheduler)
        return PolymarketPositionsPreviewLoader(
            repository = repository,
            deriveAddresses = deriveAddresses,
            updateListener = positionsUpdater,
            dispatchers = TestingCoroutineDispatcherProvider(
                main = dispatcher,
                mainImmediate = dispatcher,
                io = dispatcher,
                default = dispatcher,
                single = dispatcher,
            ),
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