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
    fun `GIVEN paused session WHEN resumed THEN retains data without refetching`() = runTest {
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
        session.resume(backgroundScope)
        runCurrent()

        // Assert
        assertThat(session.state.value).isSameInstanceAs(loaded)
        assertThat(requests.map { it.second }).containsExactly(0, 30).inOrder()
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