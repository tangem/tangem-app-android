package com.tangem.domain.polymarket.session

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionsListConfig
import com.tangem.domain.polymarket.model.PolymarketPositionsState
import com.tangem.domain.polymarket.model.PolymarketPositionsTab
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.PaginationStatus
import com.tangem.test.core.ProvideTestModels
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketPositionsSessionTest : PolymarketPositionsSessionTestBase() {

    @Test
    fun `GIVEN overlapping pages WHEN loading more THEN preserves order and advances by source pages`() = runTest {
        // Arrange
        val session = createFactory()(wallet, PolymarketPositionsTab.ACTIVE)
        val first = position(assetId = "one")
        val repeated = position(assetId = "one")
        val second = position(assetId = "two")
        val third = position(assetId = "one", conditionId = "another")
        fetch = { offset ->
            when (offset) {
                0 -> page(listOf(first), last = false)
                30 -> page(listOf(repeated, second), last = false)
                else -> page(listOf(third))
            }
        }

        // Act
        session.resume(backgroundScope)
        runCurrent()
        session.loadMore()
        runCurrent()
        session.loadMore()
        runCurrent()

        // Assert
        assertThat(session.state.value).isEqualTo(
            PolymarketPositionsState(listOf(first, second, third), PaginationStatus.EndOfPagination),
        )
        assertThat(requests.map { it.second }).containsExactly(0, 30, 60).inOrder()
    }

    @Test
    fun `GIVEN a resumed session WHEN the positions are invalidated THEN the list is fetched again`() = runTest {
        // Arrange
        val session = createFactory()(wallet, PolymarketPositionsTab.ACTIVE)
        val first = position(assetId = "one")
        val second = position(assetId = "two")
        var isReloaded = false
        fetch = { page(listOf(if (isReloaded) second else first)) }
        session.resume(backgroundScope)
        runCurrent()

        // Act
        isReloaded = true
        positionsUpdater.triggerUpdate(wallet)
        runCurrent()

        // Assert
        assertThat(session.state.value)
            .isEqualTo(PolymarketPositionsState(listOf(second), PaginationStatus.EndOfPagination))
        assertThat(requests.map { it.second }).containsExactly(0, 0).inOrder()
    }

    @Test
    fun `GIVEN the positions changed while paused WHEN the session resumes THEN it fetches them again`() = runTest {
        // Arrange
        val session = createFactory()(wallet, PolymarketPositionsTab.ACTIVE)
        val first = position(assetId = "one")
        val second = position(assetId = "two")
        var isReloaded = false
        fetch = { page(listOf(if (isReloaded) second else first)) }
        session.resume(backgroundScope)
        runCurrent()
        session.pause()

        // Act
        isReloaded = true
        positionsUpdater.triggerUpdate(wallet)
        runCurrent()
        session.resume(backgroundScope)
        runCurrent()

        // Assert
        assertThat(session.state.value)
            .isEqualTo(PolymarketPositionsState(listOf(second), PaginationStatus.EndOfPagination))
        assertThat(requests.map { it.second }).containsExactly(0, 0).inOrder()
    }

    @Test
    fun `GIVEN the positions changed before the first resume WHEN the session resumes THEN it fetches them once`() =
        runTest {
            // Arrange
            val session = createFactory()(wallet, PolymarketPositionsTab.ACTIVE)
            fetch = { page(listOf(position())) }
            positionsUpdater.triggerUpdate(wallet)

            // Act
            session.resume(backgroundScope)
            runCurrent()

            // Assert
            assertThat(requests.map { it.second }).containsExactly(0)
        }

    @Test
    fun `GIVEN loaded pages changed while paused WHEN the session resumes THEN it reloads from the start only`() =
        runTest {
            // Arrange
            val session = createFactory()(wallet, PolymarketPositionsTab.ACTIVE)
            fetch = { offset -> page(listOf(position(assetId = "at$offset")), last = offset == 30) }
            session.resume(backgroundScope)
            runCurrent()
            session.loadMore()
            runCurrent()
            session.pause()
            val reloaded = position(assetId = "reloaded")
            fetch = { page(listOf(reloaded)) }

            // Act
            positionsUpdater.triggerUpdate(wallet)
            runCurrent()
            session.resume(backgroundScope)
            runCurrent()

            // Assert
            assertThat(session.state.value)
                .isEqualTo(PolymarketPositionsState(listOf(reloaded), PaginationStatus.EndOfPagination))
            assertThat(requests.map { it.second }).containsExactly(0, 30, 0).inOrder()
        }

    @Test
    fun `GIVEN a paused session WHEN the positions are invalidated THEN nothing is fetched`() = runTest {
        // Arrange
        val session = createFactory()(wallet, PolymarketPositionsTab.ACTIVE)
        fetch = { page(listOf(position())) }
        session.resume(backgroundScope)
        runCurrent()
        session.pause()

        // Act
        positionsUpdater.triggerUpdate(wallet)
        runCurrent()

        // Assert
        assertThat(requests.map { it.second }).containsExactly(0)
    }

    @Test
    fun `GIVEN failed next page WHEN manually retried THEN retains positions and retries the same offset`() = runTest {
        // Arrange
        val session = createFactory()(wallet, PolymarketPositionsTab.ACTIVE)
        val first = position()
        val second = position(assetId = "second")
        val response = CompletableDeferred<BatchFetchResult<List<PolymarketPosition>>>()
        val error = BatchFetchResult.Error(IllegalStateException("Offline"))
        fetch = { offset -> if (offset == 0) page(listOf(first), last = false) else response.await() }
        session.resume(backgroundScope)
        runCurrent()

        // Act
        session.loadMore()
        session.loadMore()
        runCurrent()
        val loading = session.state.value
        response.complete(error)
        runCurrent()
        val failed = session.state.value
        advanceTimeBy(120_000)
        runCurrent()

        // Assert
        assertThat(loading).isEqualTo(PolymarketPositionsState(listOf(first), PaginationStatus.NextBatchLoading))
        assertThat(failed).isEqualTo(PolymarketPositionsState(listOf(first), PaginationStatus.Paginating(error)))
        assertThat(requests.map { it.second }).containsExactly(0, 30).inOrder()

        // Act
        fetch = { page(listOf(second)) }
        session.retry()
        session.retry()
        runCurrent()
        session.loadMore()
        runCurrent()

        // Assert
        assertThat(session.state.value).isEqualTo(
            PolymarketPositionsState(listOf(first, second), PaginationStatus.EndOfPagination),
        )
        assertThat(requests.map { it.second }).containsExactly(0, 30, 30).inOrder()
    }

    @Test
    fun `GIVEN initial request failed WHEN retried THEN starts at the first page`() = runTest {
        // Arrange
        val session = createFactory()(wallet, PolymarketPositionsTab.FINISHED)
        val expected = position()
        val error = IllegalStateException("Offline")
        fetch = { BatchFetchResult.Error(error) }
        session.resume(backgroundScope)
        runCurrent()
        val failed = session.state.value

        // Act
        fetch = { page(listOf(expected)) }
        session.retry()
        session.retry()
        runCurrent()

        // Assert
        assertThat(failed).isEqualTo(PolymarketPositionsState(status = PaginationStatus.InitialLoadingError(error)))
        assertThat(session.state.value).isEqualTo(
            PolymarketPositionsState(listOf(expected), PaginationStatus.EndOfPagination),
        )
        val config = PolymarketPositionsListConfig(addresses, PolymarketPositionsTab.FINISHED)
        assertThat(requests).containsExactly(config to 0, config to 0).inOrder()
    }

    @Test
    fun `GIVEN paused session with loaded pages WHEN resumed THEN re-reads them in place and keeps the pagination`() =
        runTest {
            // Arrange
            val session = createFactory()(wallet, PolymarketPositionsTab.ACTIVE)
            val first = position(assetId = "first")
            val sold = position(assetId = "sold")
            fetch = { offset -> page(listOf(if (offset == 0) first else sold), last = false) }
            session.resume(backgroundScope)
            runCurrent()
            session.loadMore()
            runCurrent()
            val loaded = session.state.value
            val refreshedFirst = position(assetId = "first")
            val third = position(assetId = "third")
            val fourth = position(assetId = "fourth")
            fetch = { offset ->
                when (offset) {
                    0 -> page(listOf(refreshedFirst), last = false)
                    30 -> page(listOf(third), last = false)
                    else -> page(listOf(fourth))
                }
            }

            // Act
            session.pause()
            session.resume(backgroundScope)
            runCurrent()

            // Assert
            assertThat(session.state.value).isEqualTo(loaded.copy(positions = listOf(refreshedFirst, third)))
            assertThat(requests.map { it.second }).containsExactly(0, 30, 0, 30).inOrder()

            // Act
            session.loadMore()
            runCurrent()

            // Assert
            assertThat(session.state.value).isEqualTo(
                PolymarketPositionsState(listOf(refreshedFirst, third, fourth), PaginationStatus.EndOfPagination),
            )
            assertThat(requests.map { it.second }).containsExactly(0, 30, 0, 30, 60).inOrder()
        }

    @Test
    fun `GIVEN paused session WHEN resumed and the re-read fails THEN keeps the loaded rows`() = runTest {
        // Arrange
        val session = createFactory()(wallet, PolymarketPositionsTab.ACTIVE)
        fetch = { page(last = false) }
        session.resume(backgroundScope)
        runCurrent()
        session.loadMore()
        runCurrent()
        val loaded = session.state.value
        fetch = { BatchFetchResult.Error(IllegalStateException("Offline")) }

        // Act
        session.pause()
        session.resume(backgroundScope)
        runCurrent()

        // Assert
        assertThat(session.state.value).isSameInstanceAs(loaded)
        assertThat(requests.map { it.second }).containsExactly(0, 30, 0).inOrder()
    }

    @Test
    fun `GIVEN paused session WHEN load more or retry requested THEN nothing is read until resumed`() = runTest {
        // Arrange
        val session = createFactory()(wallet, PolymarketPositionsTab.ACTIVE)
        fetch = { page(last = false) }
        session.resume(backgroundScope)
        runCurrent()
        session.loadMore()
        runCurrent()
        val loaded = session.state.value

        // Act
        session.pause()
        session.loadMore()
        session.retry()
        advanceTimeBy(120_000)
        runCurrent()

        // Assert
        assertThat(session.state.value).isSameInstanceAs(loaded)
        assertThat(requests.map { it.second }).containsExactly(0, 30).inOrder()
    }

    @Test
    fun `GIVEN resumed session WHEN resumed again THEN does not re-read`() = runTest {
        // Arrange
        val session = createFactory()(wallet, PolymarketPositionsTab.ACTIVE)
        session.resume(backgroundScope)
        runCurrent()

        // Act
        session.resume(backgroundScope)
        runCurrent()

        // Assert
        assertThat(requests.map { it.second }).containsExactly(0)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN disposed session WHEN an old response arrives THEN cannot publish or restart it`(
        hasLoadedPage: Boolean,
    ) = runTest {
        // Arrange
        val factory = createFactory()
        val previous = factory(wallet, PolymarketPositionsTab.ACTIVE)
        val response = CompletableDeferred<Unit>()
        val first = position()
        fetch = { offset ->
            if (hasLoadedPage && offset == 0) page(listOf(first), last = false)
            else withContext(NonCancellable) { response.await(); page() }
        }
        previous.resume(backgroundScope)
        runCurrent()
        if (hasLoadedPage) {
            previous.loadMore()
            runCurrent()
        }
        previous.dispose()
        val current = factory(wallet, PolymarketPositionsTab.ACTIVE)
        val expected = position(assetId = "new")
        fetch = { page(listOf(expected)) }

        // Act
        current.resume(backgroundScope)
        previous.resume(backgroundScope)
        previous.loadMore()
        previous.retry()
        runCurrent()
        response.complete(Unit)
        runCurrent()

        // Assert
        val previousState = if (hasLoadedPage) {
            PolymarketPositionsState(listOf(first), PaginationStatus.NextBatchLoading)
        } else {
            PolymarketPositionsState(status = PaginationStatus.InitialLoading)
        }
        assertThat(previous.state.value).isEqualTo(previousState)
        assertThat(current.state.value).isEqualTo(
            PolymarketPositionsState(listOf(expected), PaginationStatus.EndOfPagination),
        )
        assertThat(requests.map { it.second })
            .containsExactlyElementsIn(if (hasLoadedPage) listOf(0, 30, 0) else listOf(0, 0)).inOrder()
    }

    private companion object {
        @JvmStatic
        fun provideTestModels() = listOf(false, true)
    }
}