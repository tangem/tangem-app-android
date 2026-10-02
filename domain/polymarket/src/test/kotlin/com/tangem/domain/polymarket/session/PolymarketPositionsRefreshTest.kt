package com.tangem.domain.polymarket.session

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionResult
import com.tangem.domain.polymarket.model.PolymarketPositionsFilter
import com.tangem.domain.polymarket.model.PolymarketPositionsState
import com.tangem.domain.polymarket.model.PolymarketPositionsTab
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.PaginationStatus
import com.tangem.test.core.ProvideTestModels
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketPositionsRefreshTest : PolymarketPositionsSessionTestBase() {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN loaded pages WHEN manually refreshed THEN retains rows and rebuilds pagination`(tab: PolymarketPositionsTab) =
        runTest {
            // Arrange
            val session = createFactory()(wallet, tab)
            val old = position(assetId = "old")
            val removed = position(assetId = "removed")
            fetch = { offset -> page(listOf(if (offset == 0) old else removed), last = offset == 30) }
            session.resume(backgroundScope)
            runCurrent()
            session.loadMore()
            runCurrent()
            val before = session.state.value
            val response = CompletableDeferred<BatchFetchResult<List<PolymarketPosition>>>()
            val first = position(assetId = "new")
            val next = position(assetId = "next")
            fetch = { offset -> if (offset == 0) response.await() else page(listOf(next)) }

            // Act
            session.refresh()
            session.refresh()
            session.loadMore()
            runCurrent()

            // Assert
            assertThat(session.state.value).isEqualTo(before.copy(isRefreshing = true))
            assertThat(requests.map { it.second }).containsExactly(0, 30, 0).inOrder()

            // Act
            response.complete(page(listOf(first), last = false))
            runCurrent()
            session.loadMore()
            runCurrent()

            // Assert
            assertThat(session.state.value.positions).containsExactly(first, next).inOrder()
            assertThat(session.state.value.status).isEqualTo(PaginationStatus.EndOfPagination)
            assertThat(session.state.value.isRefreshing).isFalse()
            assertThat(requests.map { it.second }).containsExactly(0, 30, 0, 30).inOrder()
            assertThat(requests.map { it.first.tab }.distinct()).containsExactly(tab)
        }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN an error then empty list WHEN manually refreshed THEN can show the first position`(
        tab: PolymarketPositionsTab,
    ) = runTest {
        // Arrange
        val session = createFactory()(wallet, tab)
        fetch = { BatchFetchResult.Error(IllegalStateException("Offline")) }
        session.resume(backgroundScope)
        runCurrent()
        fetch = { page(emptyList()) }

        // Act
        session.refresh()
        runCurrent()

        // Assert
        assertThat(session.state.value.positions).isEmpty()
        assertThat(session.state.value.status).isEqualTo(PaginationStatus.EndOfPagination)
        assertThat(session.state.value.isRefreshing).isFalse()

        // Act
        val first = position()
        fetch = { page(listOf(first)) }
        session.refresh()
        runCurrent()

        // Assert
        assertThat(session.state.value.positions).containsExactly(first)
        assertThat(requests.map { it.second }).containsExactly(0, 0, 0).inOrder()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN loaded rows WHEN manual refresh fails THEN preserves them until a successful retry`(
        tab: PolymarketPositionsTab,
    ) = runTest {
        // Arrange
        val session = createFactory()(wallet, tab)
        session.resume(backgroundScope)
        runCurrent()
        val before = session.state.value
        val error = IllegalStateException("Offline")
        fetch = { BatchFetchResult.Error(error) }

        // Act
        session.refresh()
        runCurrent()
        session.loadMore()
        runCurrent()

        // Assert
        assertThat(session.state.value).isEqualTo(before.copy(refreshError = error))
        assertThat(requests.map { it.second }).containsExactly(0, 0).inOrder()

        // Act
        fetch = { page(emptyList()) }
        session.refresh()
        runCurrent()

        // Assert
        assertThat(session.state.value).isEqualTo(before.copy(positions = emptyList()))
        assertThat(requests.map { it.second }).containsExactly(0, 0, 0).inOrder()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a previously empty list WHEN resumed THEN reads the first page again`(tab: PolymarketPositionsTab) =
        runTest {
            // Arrange
            val session = createFactory()(wallet, tab)
            fetch = { page(emptyList()) }
            session.resume(backgroundScope)
            runCurrent()
            session.pause()
            val first = position()
            fetch = { page(listOf(first)) }

            // Act
            session.resume(backgroundScope)
            runCurrent()

            // Assert
            assertThat(session.state.value.positions).containsExactly(first)
            assertThat(requests.map { it.second }).containsExactly(0, 0).inOrder()
        }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN an older resume request WHEN a newer response arrives first THEN the older one is discarded`(
        tab: PolymarketPositionsTab,
    ) = runTest {
        // Arrange
        val session = createFactory()(wallet, tab)
        session.resume(backgroundScope)
        runCurrent()
        session.pause()
        val oldResponse = CompletableDeferred<Unit>()
        fetch = { withContext(NonCancellable) { oldResponse.await(); page() } }
        session.resume(backgroundScope)
        runCurrent()
        session.pause()
        val fresh = position(assetId = "fresh")
        fetch = { page(listOf(fresh)) }

        // Act
        session.resume(backgroundScope)
        runCurrent()
        oldResponse.complete(Unit)
        runCurrent()

        // Assert
        assertThat(session.state.value.positions).containsExactly(fresh)
        assertThat(requests.map { it.second }).containsExactly(0, 0, 0).inOrder()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a delayed automatic refresh WHEN pulled to refresh THEN only the manual result is applied`(
        tab: PolymarketPositionsTab,
    ) = runTest {
        // Arrange
        val session = createFactory()(wallet, tab)
        session.resume(backgroundScope)
        runCurrent()
        session.pause()
        val oldResponse = CompletableDeferred<Unit>()
        fetch = { withContext(NonCancellable) { oldResponse.await(); page() } }
        session.resume(backgroundScope)
        runCurrent()
        val fresh = position(assetId = "fresh")
        fetch = { page(listOf(fresh)) }

        // Act
        session.refresh()
        runCurrent()
        oldResponse.complete(Unit)
        runCurrent()

        // Assert
        assertThat(session.state.value.positions).containsExactly(fresh)
        assertThat(session.state.value.isRefreshing).isFalse()
        assertThat(requests.map { it.second }).containsExactly(0, 0, 0).inOrder()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a manual refresh in flight WHEN paused THEN ignores its response and resumes with a new read`(
        tab: PolymarketPositionsTab,
    ) = runTest {
        // Arrange
        val session = createFactory()(wallet, tab)
        session.resume(backgroundScope)
        runCurrent()
        val before = session.state.value
        val response = CompletableDeferred<Unit>()
        fetch = { withContext(NonCancellable) { response.await(); page(emptyList()) } }
        session.refresh()
        runCurrent()

        // Act
        session.pause()
        session.refresh()
        response.complete(Unit)
        runCurrent()

        // Assert
        assertThat(session.state.value).isEqualTo(before)
        assertThat(requests.map { it.second }).containsExactly(0, 0).inOrder()

        // Act
        val fresh = position(assetId = "fresh")
        fetch = { page(listOf(fresh)) }
        session.resume(backgroundScope)
        runCurrent()

        // Assert
        assertThat(session.state.value.positions).containsExactly(fresh)
        assertThat(requests.map { it.second }).containsExactly(0, 0, 0).inOrder()
    }

    @Test
    fun `GIVEN the Won filter WHEN manually refreshed THEN keeps the filter until matching pages are read`() = runTest {
        // Arrange
        val session = createFactory()(wallet, PolymarketPositionsTab.ACTIVE)
        val oldWon = position(result = PolymarketPositionResult.WON)
        fetch = { page(listOf(oldWon)) }
        session.resume(backgroundScope)
        session.selectFilter(PolymarketPositionsFilter.WON)
        runCurrent()
        val before = session.state.value
        val nextPage = CompletableDeferred<BatchFetchResult<List<PolymarketPosition>>>()
        fetch = { offset -> if (offset == 0) page(last = false) else nextPage.await() }

        // Act
        session.refresh()
        runCurrent()

        // Assert
        assertThat(session.state.value).isEqualTo(before.copy(isRefreshing = true))

        // Act
        val newWon = position(assetId = "new", result = PolymarketPositionResult.WON)
        nextPage.complete(page(listOf(newWon)))
        runCurrent()

        // Assert
        assertThat(session.state.value).isEqualTo(before.copy(positions = listOf(newWon)))
        assertThat(requests.map { it.second }).containsExactly(0, 0, 30).inOrder()
    }

    @Test
    fun `GIVEN a refresh in flight WHEN the filter changes THEN ignores the old response and completes the new selection`() =
        runTest {
            // Arrange
            val session = createFactory()(wallet, PolymarketPositionsTab.ACTIVE)
            val oldWon = position(result = PolymarketPositionResult.WON)
            fetch = { page(listOf(oldWon)) }
            session.resume(backgroundScope)
            runCurrent()
            val oldResponse = CompletableDeferred<Unit>()
            fetch = { withContext(NonCancellable) { oldResponse.await(); page(listOf(oldWon)) } }
            session.refresh()
            runCurrent()
            val newLost = position(assetId = "lost", result = PolymarketPositionResult.LOST)
            fetch = { page(listOf(newLost)) }

            // Act
            session.selectFilter(PolymarketPositionsFilter.LOST)
            runCurrent()
            oldResponse.complete(Unit)
            runCurrent()

            // Assert
            assertThat(session.state.value).isEqualTo(
                PolymarketPositionsState(
                    positions = listOf(newLost),
                    status = PaginationStatus.EndOfPagination,
                    filter = PolymarketPositionsFilter.LOST,
                ),
            )
            assertThat(requests.map { it.second }).containsExactly(0, 0, 0).inOrder()
        }

    @Test
    fun `GIVEN a filtered refresh WHEN a later page fails THEN retains previous matches and retries from the first page`() =
        runTest {
            // Arrange
            val session = createFactory()(wallet, PolymarketPositionsTab.ACTIVE)
            val oldWon = position(result = PolymarketPositionResult.WON)
            fetch = { page(listOf(oldWon)) }
            session.resume(backgroundScope)
            session.selectFilter(PolymarketPositionsFilter.WON)
            runCurrent()
            val before = session.state.value
            val error = IllegalStateException("Offline")
            fetch = { offset -> if (offset == 0) page(last = false) else BatchFetchResult.Error(error) }

            // Act
            session.refresh()
            runCurrent()
            session.loadMore()
            runCurrent()

            // Assert
            assertThat(session.state.value).isEqualTo(before.copy(refreshError = error))
            assertThat(requests.map { it.second }).containsExactly(0, 0, 30).inOrder()

            // Act
            val newWon = position(assetId = "new", result = PolymarketPositionResult.WON)
            fetch = { offset -> if (offset == 0) page(last = false) else page(listOf(newWon)) }
            session.refresh()
            runCurrent()

            // Assert
            assertThat(session.state.value).isEqualTo(before.copy(positions = listOf(newWon)))
            assertThat(requests.map { it.second }).containsExactly(0, 0, 30, 0, 30).inOrder()
        }

    private companion object {
        @JvmStatic
        fun provideTestModels() = PolymarketPositionsTab.entries
    }
}