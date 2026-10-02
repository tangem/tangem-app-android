package com.tangem.domain.polymarket.session

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketActivityRepository
import com.tangem.domain.polymarket.model.PolymarketActivity
import com.tangem.domain.polymarket.model.PolymarketActivityBatchingContext
import com.tangem.domain.polymarket.model.PolymarketActivityError
import com.tangem.domain.polymarket.model.PolymarketActivityFilter
import com.tangem.domain.polymarket.model.PolymarketActivityListConfig
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.usecase.CreatePolymarketActivitySessionUseCase
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListSource
import com.tangem.pagination.PaginationStatus
import com.tangem.pagination.fetcher.LimitOffsetBatchFetcher
import com.tangem.pagination.toBatchFlow
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketActivitySessionTest {

    private val repository: PolymarketActivityRepository = mockk()
    private val deriveAddresses: DerivePolymarketAddressesUseCase = mockk()
    private val wallet = UserWalletId("011")
    private val addresses: PolymarketAddresses = mockk()
    private val requests = mutableListOf<Pair<PolymarketActivityFilter, Int>>()
    private var fetch: suspend (PolymarketActivityListConfig, Int) -> BatchFetchResult<List<PolymarketActivity>> =
        { _, _ -> page() }

    @Test
    fun `GIVEN overlapping and empty pages WHEN loading THEN skips empty pages and deduplicates operation ids`() = runTest {
        // Arrange
        val first = activity("first")
        val second = activity("second")
        fetch = { _, offset ->
            when (offset) {
                0 -> page(emptyList(), last = false)
                30 -> page(listOf(first), last = false)
                else -> page(listOf(first, second))
            }
        }
        val session = createSession()

        // Act
        session.resume(backgroundScope)
        runCurrent()
        session.loadMore()
        session.loadMore()
        runCurrent()

        // Assert
        assertThat(session.state.value.items).containsExactly(first, second).inOrder()
        assertThat(session.state.value.status).isEqualTo(PaginationStatus.EndOfPagination)
        assertThat(requests.map { it.second }).containsExactly(0, 30, 60).inOrder()
    }

    @Test
    fun `GIVEN loaded rows WHEN later pages add no unique operations THEN continues until new rows or the end`() = runTest {
        // Arrange
        val first = activity("first")
        val second = activity("second")
        fetch = { _, offset ->
            when (offset) {
                0 -> page(listOf(first), last = false)
                30 -> page(emptyList(), last = false)
                60 -> page(listOf(first), last = false)
                else -> page(listOf(second))
            }
        }
        val session = createSession()
        session.resume(backgroundScope)
        runCurrent()

        // Act
        session.loadMore()
        runCurrent()

        // Assert
        assertThat(session.state.value.items).containsExactly(first, second).inOrder()
        assertThat(session.state.value.status).isEqualTo(PaginationStatus.EndOfPagination)
        assertThat(requests.map { it.second }).containsExactly(0, 30, 60, 90).inOrder()
    }

    @Test
    fun `GIVEN cached history WHEN refresh fails THEN keeps rows and retries the refresh from the first page`() = runTest {
        // Arrange
        val old = activity("old")
        fetch = { _, _ -> page(listOf(old)) }
        val session = createSession()
        session.resume(backgroundScope)
        runCurrent()
        fetch = { _, _ -> BatchFetchResult.Error(IllegalStateException("offline")) }

        // Act
        session.refresh()
        runCurrent()

        // Assert
        assertThat(session.state.value.items).containsExactly(old)
        assertThat(session.state.value.isRefreshing).isFalse()
        assertThat(session.state.value.status).isInstanceOf(PaginationStatus.InitialLoadingError::class.java)

        // Act
        val fresh = activity("fresh")
        fetch = { _, _ -> page(listOf(fresh)) }
        session.retry()
        runCurrent()

        // Assert
        assertThat(session.state.value.items).containsExactly(fresh)
        assertThat(requests.map { it.second }).containsExactly(0, 0, 0).inOrder()
    }

    @Test
    fun `GIVEN multiple loaded pages WHEN refreshing THEN replaces them only after enough rows are fetched`() = runTest {
        // Arrange
        val old = listOf(activity("old-1"), activity("old-2"))
        fetch = { _, offset -> page(listOf(old[if (offset == 0) 0 else 1]), last = offset != 0) }
        val session = createSession()
        session.resume(backgroundScope)
        runCurrent()
        session.loadMore()
        runCurrent()
        val gate = CompletableDeferred<Unit>()
        val fresh = listOf(activity("new-1"), activity("new-2"))
        fetch = { _, offset ->
            if (offset == 0) page(listOf(fresh[0]), last = false) else {
                gate.await()
                page(listOf(fresh[1]))
            }
        }

        // Act
        session.refresh()
        runCurrent()

        // Assert
        assertThat(session.state.value.items).containsExactlyElementsIn(old).inOrder()
        assertThat(session.state.value.isRefreshing).isTrue()

        // Act
        gate.complete(Unit)
        runCurrent()

        // Assert
        assertThat(session.state.value.items).containsExactlyElementsIn(fresh).inOrder()
        assertThat(session.state.value.isRefreshing).isFalse()
    }

    @Test
    fun `GIVEN a slow old filter WHEN selection changes THEN its late response cannot replace the new filter`() = runTest {
        // Arrange
        val gate = CompletableDeferred<Unit>()
        val selected = PolymarketActivityFilter(PolymarketActivity.Type.BUY)
        val bought = activity("bought")
        fetch = { config, _ ->
            if (config.filter == selected) page(listOf(bought)) else withContext(NonCancellable) {
                gate.await()
                page(listOf(activity("old-filter")))
            }
        }
        val session = createSession()
        session.resume(backgroundScope)
        runCurrent()

        // Act
        session.selectFilter(selected)
        runCurrent()
        gate.complete(Unit)
        runCurrent()

        // Assert
        assertThat(session.state.value.filter).isEqualTo(selected)
        assertThat(session.state.value.items).containsExactly(bought)
        assertThat(requests.map { it.first }).containsExactly(PolymarketActivityFilter(), selected).inOrder()
    }

    @Test
    fun `GIVEN an accepted scan WHEN its delay expires THEN rereads once without submitting another scan`() = runTest {
        // Arrange
        val session = createSession()
        coEvery { repository.reconcileDeposits(addresses) } returns Either.Right(10.seconds)
        session.resume(backgroundScope)
        runCurrent()

        // Act
        advanceTimeBy(9999)
        runCurrent()

        // Assert
        assertThat(requests).hasSize(1)

        // Act
        advanceTimeBy(1)
        runCurrent()
        advanceTimeBy(60000)
        runCurrent()

        // Assert
        assertThat(requests).hasSize(2)
        coVerify(exactly = 1) { repository.reconcileDeposits(addresses) }
    }

    @Test
    fun `GIVEN a pending scan WHEN refreshing and pausing THEN avoids duplicate scans and cancels the delayed read`() = runTest {
        // Arrange
        val session = createSession()
        coEvery { repository.reconcileDeposits(addresses) } returns Either.Right(10.seconds)
        session.resume(backgroundScope)
        session.resume(backgroundScope)
        runCurrent()

        // Act
        session.refresh()
        runCurrent()
        session.pause()
        advanceTimeBy(20000)
        runCurrent()

        // Assert
        assertThat(requests).hasSize(2)
        assertThat(session.state.value.isRefreshing).isFalse()
        coVerify(exactly = 1) { repository.reconcileDeposits(addresses) }
    }

    @Test
    fun `GIVEN missing stored addresses WHEN opening history THEN reports an error without requesting a card`() = runTest {
        // Arrange
        val session = createSession()
        coEvery { deriveAddresses.stored(wallet) } returns null

        // Act
        session.resume(backgroundScope)
        runCurrent()

        // Assert
        assertThat(session.state.value.status).isInstanceOf(PaginationStatus.InitialLoadingError::class.java)
        coVerify(exactly = 0) { deriveAddresses.invoke(any()) }
        coVerify(exactly = 0) { repository.reconcileDeposits(any()) }
        verify(exactly = 0) { repository.getActivityBatchFlow(any(), any()) }
    }

    private fun TestScope.createSession(): PolymarketActivitySession {
        val dispatchers = createTestingCoroutineDispatcherProvider()
        coEvery { deriveAddresses.stored(wallet) } returns addresses
        coEvery { repository.reconcileDeposits(addresses) } returns Either.Left(PolymarketActivityError.Network)
        every { repository.getActivityBatchFlow(any(), any()) } answers {
            val context = firstArg<PolymarketActivityBatchingContext>()
            val size = secondArg<Int>()
            BatchListSource(
                fetchDispatcher = dispatchers.io,
                context = context,
                generateNewKey = { keys -> keys.size },
                batchFetcher = LimitOffsetBatchFetcher<PolymarketActivityListConfig, List<PolymarketActivity>>(
                    prefetchDistance = size,
                    batchSize = size,
                    subFetcher = { request, _, _ ->
                        requests += request.params.filter to request.offset
                        fetch(request.params, request.offset)
                    },
                ),
            ).toBatchFlow()
        }
        return CreatePolymarketActivitySessionUseCase(repository, deriveAddresses, dispatchers)(wallet)
    }

    private fun activity(id: String = "activity") = mockk<PolymarketActivity> { every { this@mockk.id } returns id }

    private fun page(items: List<PolymarketActivity> = emptyList(), last: Boolean = true) =
        BatchFetchResult.Success(data = items, empty = items.isEmpty(), last = last)
}