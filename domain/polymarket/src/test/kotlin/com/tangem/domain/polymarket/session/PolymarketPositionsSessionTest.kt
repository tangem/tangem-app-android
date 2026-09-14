package com.tangem.domain.polymarket.session

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionId
import com.tangem.domain.polymarket.model.PolymarketPositionsBatchingContext
import com.tangem.domain.polymarket.model.PolymarketPositionsListConfig
import com.tangem.domain.polymarket.model.PolymarketPositionsState
import com.tangem.domain.polymarket.model.PolymarketPositionsTab
import com.tangem.domain.polymarket.usecase.CreatePolymarketPositionsSessionUseCase
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListSource
import com.tangem.pagination.PaginationStatus
import com.tangem.pagination.fetcher.LimitOffsetBatchFetcher
import com.tangem.pagination.toBatchFlow
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketPositionsSessionTest {

    private val repository: PolymarketPositionsRepository = mockk()
    private val deriveAddresses: DerivePolymarketAddressesUseCase = mockk()
    private val wallet = UserWalletId("011")
    private val addresses: PolymarketAddresses = mockk()
    private val requests = mutableListOf<Pair<PolymarketPositionsListConfig, Int>>()
    private var fetch: suspend (Int) -> BatchFetchResult<List<PolymarketPosition>> = { page() }

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

    private fun TestScope.createFactory(): CreatePolymarketPositionsSessionUseCase {
        val dispatcher = StandardTestDispatcher(testScheduler)
        coEvery { deriveAddresses.stored(wallet) } returns addresses
        every { repository.getPositionsBatchFlow(any(), any()) } answers {
            val context = firstArg<PolymarketPositionsBatchingContext>()
            val size = secondArg<Int>()
            BatchListSource(
                fetchDispatcher = dispatcher,
                context = context,
                generateNewKey = { keys -> keys.size },
                batchFetcher = LimitOffsetBatchFetcher<PolymarketPositionsListConfig, List<PolymarketPosition>>(
                    prefetchDistance = size,
                    batchSize = size,
                    subFetcher = { request, _, _ ->
                        requests += request.params to request.offset
                        fetch(request.offset)
                    },
                ),
            ).toBatchFlow()
        }
        return CreatePolymarketPositionsSessionUseCase(
            repository,
            deriveAddresses,
            TestingCoroutineDispatcherProvider(dispatcher, dispatcher, dispatcher, dispatcher, dispatcher),
        )
    }

    private fun position(assetId: String = "asset", conditionId: String = "condition"): PolymarketPosition =
        mockk<PolymarketPosition.Open> {
            every { id } returns PolymarketPositionId(conditionId, assetId)
        }

    private fun page(
        positions: List<PolymarketPosition> = listOf(position()),
        last: Boolean = true,
    ): BatchFetchResult<List<PolymarketPosition>> = BatchFetchResult.Success(positions, positions.isEmpty(), last)

    private companion object {
        @JvmStatic
        fun provideTestModels() = listOf(false, true)
    }
}