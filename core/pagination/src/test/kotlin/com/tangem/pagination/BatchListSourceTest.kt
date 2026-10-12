package com.tangem.pagination

import com.google.common.truth.Truth.assertThat
import com.tangem.pagination.fetcher.LimitOffsetBatchFetcher
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class BatchListSourceTest {

    @Test
    fun `GIVEN immediate page consumer WHEN ready is published THEN accepts the next request`() = runTest {
        // Arrange
        val requested = mutableListOf<Int>()
        val (source, actions) = createSource { offset ->
            requested += offset
            BatchFetchResult.Success(listOf(offset), empty = false, last = false)
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            source.state.collect { state ->
                if (state.status is PaginationStatus.Paginating && state.data.size < 3) {
                    actions.emit(BatchAction.LoadMore())
                    actions.emit(BatchAction.LoadMore())
                }
            }
        }

        // Act
        actions.emit(BatchAction.Reload("positions"))
        runCurrent()

        // Assert
        assertThat(requested).containsExactly(0, 1, 2).inOrder()
        assertThat(source.state.value).isEqualTo(
            BatchListState(
                data = listOf(Batch(0, listOf(0)), Batch(1, listOf(1)), Batch(2, listOf(2))),
                status = PaginationStatus.Paginating(BatchFetchResult.Success(listOf(2), empty = false, last = false)),
            ),
        )
    }

    @Test
    fun `GIVEN page in flight WHEN load more is repeated THEN requests it once`() = runTest {
        // Arrange
        val requested = mutableListOf<Int>()
        val response = CompletableDeferred<Unit>()
        val (source, actions) = createSource { offset ->
            requested += offset
            if (offset > 0) response.await()
            BatchFetchResult.Success(listOf(offset), empty = false, last = offset > 0)
        }
        actions.emit(BatchAction.Reload("positions"))
        runCurrent()

        // Act
        actions.emit(BatchAction.LoadMore())
        actions.emit(BatchAction.LoadMore())
        runCurrent()
        actions.emit(BatchAction.LoadMore())
        runCurrent()
        response.complete(Unit)
        runCurrent()

        // Assert
        assertThat(requested).containsExactly(0, 1).inOrder()
        assertThat(source.state.value.status).isEqualTo(PaginationStatus.EndOfPagination)
    }

    @Test
    fun `GIVEN reset source WHEN a cancelled page completes THEN keeps the reset state`() = runTest {
        // Arrange
        val response = CompletableDeferred<Unit>()
        val (source, actions) = createSource { offset ->
            if (offset > 0) withContext(NonCancellable) { response.await() }
            BatchFetchResult.Success(listOf(offset), empty = false, last = offset > 0)
        }
        actions.emit(BatchAction.Reload("positions"))
        runCurrent()
        actions.emit(BatchAction.LoadMore())
        runCurrent()

        // Act
        actions.emit(BatchAction.Reset)
        runCurrent()
        response.complete(Unit)
        runCurrent()

        // Assert
        assertThat(source.state.value).isEqualTo(BatchListState<Int, List<Int>>(emptyList(), PaginationStatus.None))
    }

    private fun TestScope.createSource(
        fetch: suspend (Int) -> BatchFetchResult<List<Int>>,
    ): Pair<BatchListSource<Int, List<Int>, Nothing>, MutableSharedFlow<BatchAction<Int, String, Nothing>>> {
        val actions = MutableSharedFlow<BatchAction<Int, String, Nothing>>(replay = 1)
        val source = BatchListSource(
            fetchDispatcher = StandardTestDispatcher(testScheduler),
            context = BatchingContext(actions, CoroutineScope(backgroundScope.coroutineContext +
                UnconfinedTestDispatcher(testScheduler))),
            generateNewKey = { keys -> keys.size },
            batchFetcher = LimitOffsetBatchFetcher<String, List<Int>>(
                prefetchDistance = 1,
                batchSize = 1,
                subFetcher = { request, _, _ -> fetch(request.offset) },
            ),
        )
        return source to actions
    }
}