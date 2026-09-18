package com.tangem.domain.polymarket.session

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketPositionResult
import com.tangem.domain.polymarket.model.PolymarketPositionsFilter
import com.tangem.domain.polymarket.model.PolymarketPositionsState
import com.tangem.domain.polymarket.model.PolymarketPositionsTab
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.PaginationStatus
import com.tangem.test.core.ProvideTestModels
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketPositionsFilteringTest : PolymarketPositionsSessionTestBase() {

    @Test
    fun `GIVEN Won filter WHEN resumed and a row is no longer won THEN it leaves the filtered list`() = runTest {
        // Arrange
        val session = createFactory()(wallet, PolymarketPositionsTab.ACTIVE)
        val won = position(assetId = "won", result = PolymarketPositionResult.WON)
        val stillWon = position(assetId = "still", result = PolymarketPositionResult.WON)
        fetch = { page(listOf(won, stillWon)) }
        session.selectFilter(PolymarketPositionsFilter.WON)
        session.resume(backgroundScope)
        runCurrent()
        val reopened = position(assetId = "won")
        fetch = { page(listOf(reopened, stillWon)) }

        // Act
        session.pause()
        session.resume(backgroundScope)
        runCurrent()

        // Assert
        assertThat(session.state.value).isEqualTo(
            PolymarketPositionsState(listOf(stillWon), PaginationStatus.EndOfPagination, PolymarketPositionsFilter.WON),
        )
        assertThat(requests.map { it.second }).containsExactly(0, 0).inOrder()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN rare matches WHEN filter selected THEN continues through unmatched and empty pages`(
        immediateCollection: Boolean,
    ) = runTest {
        // Arrange
        val owner = if (immediateCollection) UnconfinedTestDispatcher(testScheduler)
        else StandardTestDispatcher(testScheduler)
        val session = createFactory(owner, StandardTestDispatcher(testScheduler))(wallet, PolymarketPositionsTab.ACTIVE)
        val first = position(assetId = "first", result = PolymarketPositionResult.WON)
        val second = position(assetId = "second", result = PolymarketPositionResult.WON)
        val lastResponse = CompletableDeferred<Unit>()
        fetch = { offset ->
            when (offset) {
                0 -> page(last = false)
                30 -> page(emptyList(), last = false)
                60 -> page(listOf(first), last = false)
                else -> {
                    lastResponse.await()
                    page(listOf(second))
                }
            }
        }

        // Act
        session.selectFilter(PolymarketPositionsFilter.WON)
        session.resume(CoroutineScope(backgroundScope.coroutineContext + owner))
        runCurrent()
        val partial = session.state.value
        lastResponse.complete(Unit)
        runCurrent()

        // Assert
        assertThat(partial).isEqualTo(
            PolymarketPositionsState(listOf(first), PaginationStatus.NextBatchLoading, PolymarketPositionsFilter.WON),
        )
        assertThat(session.state.value).isEqualTo(
            PolymarketPositionsState(
                listOf(first, second), PaginationStatus.EndOfPagination, PolymarketPositionsFilter.WON,
            ),
        )
        assertThat(requests.map { it.second }).containsExactly(0, 30, 60, 90).inOrder()
    }

    @Test
    fun `GIVEN enough matches WHEN loading more THEN requests one more visible batch`() = runTest {
        // Arrange
        val session = createFactory()(wallet, PolymarketPositionsTab.ACTIVE)
        val winners = List(60) { position(assetId = "$it", result = PolymarketPositionResult.WON) }
        fetch = { offset ->
            when (offset) {
                0 -> page(last = false)
                30 -> page(winners.take(30), last = false)
                else -> page(winners.drop(30), last = false)
            }
        }
        session.resume(backgroundScope)
        runCurrent()

        // Act
        session.selectFilter(PolymarketPositionsFilter.WON)
        runCurrent()

        // Assert
        assertThat(session.state.value.positions).containsExactlyElementsIn(winners.take(30)).inOrder()
        assertThat(session.state.value.status).isInstanceOf(PaginationStatus.Paginating::class.java)
        assertThat(requests.map { it.second }).containsExactly(0, 30).inOrder()

        // Act
        session.loadMore()
        session.loadMore()
        runCurrent()

        // Assert
        assertThat(session.state.value.positions).containsExactlyElementsIn(winners).inOrder()
        assertThat(requests.map { it.second }).containsExactly(0, 30, 60).inOrder()
    }

    @Test
    fun `GIVEN request in flight WHEN filter changes THEN publishes current matches and retains all fetched data`() =
        runTest {
            // Arrange
            val session = createFactory()(wallet, PolymarketPositionsTab.ACTIVE)
            val won = position(assetId = "won", result = PolymarketPositionResult.WON)
            val lost = position(assetId = "lost", result = PolymarketPositionResult.LOST)
            val nextWon = position(assetId = "nextWon", result = PolymarketPositionResult.WON)
            val nextLost = position(assetId = "nextLost", result = PolymarketPositionResult.LOST)
            val response = CompletableDeferred<Unit>()
            fetch = { offset ->
                if (offset == 0) page(listOf(won, lost), last = false) else {
                    response.await()
                    page(listOf(nextWon, nextLost))
                }
            }
            session.resume(backgroundScope)
            runCurrent()
            session.selectFilter(PolymarketPositionsFilter.WON)
            runCurrent()

            // Act
            session.selectFilter(PolymarketPositionsFilter.ALL)
            session.selectFilter(PolymarketPositionsFilter.LOST)
            runCurrent()
            val pending = session.state.value
            response.complete(Unit)
            runCurrent()

            // Assert
            assertThat(pending).isEqualTo(
                PolymarketPositionsState(listOf(lost), PaginationStatus.NextBatchLoading, PolymarketPositionsFilter.LOST),
            )
            assertThat(session.state.value).isEqualTo(
                PolymarketPositionsState(
                    listOf(lost, nextLost), PaginationStatus.EndOfPagination, PolymarketPositionsFilter.LOST,
                ),
            )

            // Act
            session.selectFilter(PolymarketPositionsFilter.ALL)
            runCurrent()

            // Assert
            assertThat(session.state.value).isEqualTo(
                PolymarketPositionsState(listOf(won, lost, nextWon, nextLost), PaginationStatus.EndOfPagination),
            )
            assertThat(requests.map { it.second }).containsExactly(0, 30).inOrder()
        }

    @Test
    fun `GIVEN failed search page WHEN retried THEN keeps matches and the failed offset`() = runTest {
        // Arrange
        val session = createFactory()(wallet, PolymarketPositionsTab.ACTIVE)
        val first = position(result = PolymarketPositionResult.WON)
        val second = position(assetId = "second", result = PolymarketPositionResult.WON)
        val error = BatchFetchResult.Error(IllegalStateException("Offline"))
        fetch = { offset -> if (offset == 0) page(listOf(first), last = false) else error }
        session.selectFilter(PolymarketPositionsFilter.WON)
        session.resume(backgroundScope)
        runCurrent()

        // Act
        session.selectFilter(PolymarketPositionsFilter.LOST)
        runCurrent()
        val emptyError = session.state.value
        session.selectFilter(PolymarketPositionsFilter.WON)
        runCurrent()
        advanceTimeBy(120_000)
        runCurrent()

        // Assert
        assertThat(emptyError).isEqualTo(
            PolymarketPositionsState(emptyList(), PaginationStatus.Paginating(error), PolymarketPositionsFilter.LOST),
        )
        assertThat(session.state.value.positions).containsExactly(first)
        assertThat(requests.map { it.second }).containsExactly(0, 30).inOrder()

        // Act
        fetch = { page(listOf(second)) }
        session.retry()
        runCurrent()

        // Assert
        assertThat(session.state.value).isEqualTo(
            PolymarketPositionsState(
                listOf(first, second), PaginationStatus.EndOfPagination, PolymarketPositionsFilter.WON,
            ),
        )
        assertThat(requests.map { it.second }).containsExactly(0, 30, 30).inOrder()
    }

    @Test
    fun `GIVEN no matching positions WHEN source ends THEN confirms an empty result`() = runTest {
        // Arrange
        val session = createFactory()(wallet, PolymarketPositionsTab.ACTIVE)
        val response = CompletableDeferred<Unit>()
        fetch = { offset ->
            if (offset == 0) page(last = false) else {
                response.await()
                page()
            }
        }
        session.selectFilter(PolymarketPositionsFilter.WON)
        session.resume(backgroundScope)
        runCurrent()
        val pending = session.state.value

        // Act
        response.complete(Unit)
        runCurrent()

        // Assert
        assertThat(pending).isEqualTo(
            PolymarketPositionsState(status = PaginationStatus.NextBatchLoading, filter = PolymarketPositionsFilter.WON),
        )
        assertThat(session.state.value).isEqualTo(
            PolymarketPositionsState(status = PaginationStatus.EndOfPagination, filter = PolymarketPositionsFilter.WON),
        )
    }

    @Test
    fun `GIVEN paused filtering WHEN resumed THEN continues the same search and new visits reset it`() = runTest {
        // Arrange
        val factory = createFactory()
        val session = factory(wallet, PolymarketPositionsTab.ACTIVE)
        val first = position(assetId = "first", result = PolymarketPositionResult.WON)
        val second = position(assetId = "second", result = PolymarketPositionResult.WON)
        val response = CompletableDeferred<Unit>()
        fetch = { offset ->
            when (offset) {
                0 -> page(last = false)
                30 -> {
                    response.await()
                    page(listOf(first), last = false)
                }
                else -> page(listOf(second))
            }
        }
        session.selectFilter(PolymarketPositionsFilter.WON)
        session.resume(backgroundScope)
        runCurrent()

        // Act
        session.pause()
        response.complete(Unit)
        runCurrent()

        // Assert
        assertThat(session.state.value.positions).containsExactly(first)
        assertThat(requests.map { it.second }).containsExactly(0, 30).inOrder()

        // Act
        session.resume(backgroundScope)
        runCurrent()
        val completed = session.state.value
        session.dispose()
        val reopened = factory(wallet, PolymarketPositionsTab.ACTIVE)
        reopened.resume(backgroundScope)
        runCurrent()

        // Assert
        assertThat(completed).isEqualTo(
            PolymarketPositionsState(
                listOf(first, second), PaginationStatus.EndOfPagination, PolymarketPositionsFilter.WON,
            ),
        )
        assertThat(reopened.state.value.filter).isEqualTo(PolymarketPositionsFilter.ALL)
        assertThat(requests.map { it.second }).containsExactly(0, 30, 0, 30, 60, 0).inOrder()
    }

    private companion object {
        @JvmStatic
        fun provideTestModels() = listOf(false, true)
    }
}