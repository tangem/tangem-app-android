package com.tangem.pagination

import com.tangem.pagination.fetcher.BatchFetcher
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

/**
 * Source for paginated data. The starting point for pagination.
 *
 * @param TKey Type of the key that identifies a batch.
 * @param TData Type of the data in the batch. Generally, it' a list of items.
 * @param TUpdate Type of the update request.
 * @property state State of the paginated data.
 * @property updateResults Flow of results of update requests.
 */
interface BatchListSource<TKey, TData, TUpdate> {
    val state: StateFlow<BatchListState<TKey, TData>>
    val updateResults: SharedFlow<Pair<TUpdate, BatchUpdateResult<TKey, TData>>>
}

/**
 * Creates a new [BatchListSource] with the provided configuration.
 *
 * @param fetchDispatcher Dispatcher for fetch operations.
 * @param context Context for batching.
 * @param generateNewKey Function to generate a new key for a batch.
 * @param batchFetcher Function to fetch a batch of data.
 *
 * @return New instance of [BatchListSource].
 */
@Suppress("FunctionNaming")
fun <TKey, TData, TRequestParams : Any> BatchListSource(
    fetchDispatcher: CoroutineDispatcher = Dispatchers.IO,
    context: BatchingContext<TKey, TRequestParams, Nothing>,
    generateNewKey: suspend (List<TKey>) -> TKey,
    batchFetcher: BatchFetcher<TRequestParams, TData>,
): BatchListSource<TKey, TData, Nothing> =
    DefaultBatchListSource(fetchDispatcher, context, generateNewKey, batchFetcher, null)

/**
 * Creates a new [BatchListSource] with the provided configuration.
 *
 * @param fetchDispatcher Dispatcher for fetch operations.
 * @param context Context for batching.
 * @param generateNewKey Function to generate a new key for a batch.
 * @param batchFetcher Function to fetch a batch of data.
 * @param updateFetcher Function to fetch updates for batches.
 *
 * @return New instance of [BatchListSource].
 */
@Suppress("FunctionNaming")
fun <TKey, TData, TRequestParams : Any, TUpdate> BatchListSource(
    fetchDispatcher: CoroutineDispatcher = Dispatchers.IO,
    context: BatchingContext<TKey, TRequestParams, TUpdate>,
    generateNewKey: suspend (List<TKey>) -> TKey,
    batchFetcher: BatchFetcher<TRequestParams, TData>,
    updateFetcher: BatchUpdateFetcher<TKey, TData, TUpdate>,
): BatchListSource<TKey, TData, TUpdate> =
    DefaultBatchListSource(fetchDispatcher, context, generateNewKey, batchFetcher, updateFetcher)

private class DefaultBatchListSource<TKey, TData, TRequestParams : Any, TUpdate>(
    private val fetchDispatcher: CoroutineDispatcher,
    private val context: BatchingContext<TKey, TRequestParams, TUpdate>,
    private val generateNewKey: suspend (List<TKey>) -> TKey,
    private val batchFetcher: BatchFetcher<TRequestParams, TData>,
    private val updateFetcher: BatchUpdateFetcher<TKey, TData, TUpdate>? = null,
) : BatchListSource<TKey, TData, TUpdate> {

    override val state = MutableStateFlow(BatchListState<TKey, TData>(emptyList(), PaginationStatus.None))
    override val updateResults = MutableSharedFlow<Pair<TUpdate, BatchUpdateResult<TKey, TData>>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val scope = context.coroutineScope
    private val updateJobs = MutableStateFlow<List<Pair<BatchAction.UpdateBatches<TKey, TUpdate>, Job>>>(emptyList())
    private val waitingUpdateJobs =
        MutableStateFlow<List<Pair<BatchAction.UpdateBatches<TKey, TUpdate>, Job>>>(emptyList())

    private val lastRequestResult = MutableStateFlow<BatchFetchResult<TData>?>(null)
    private var reloadActionJob: Job? = null
    private var loadMoreActionJob: Job? = null

    init {
        scope.launch {
            try {
                awaitCancellation()
            } finally {
                withContext(NonCancellable) {
                    loadMoreActionJob = null
                    loadMoreActionJob = null
                    lastRequestResult.value = null
                    stopAllUpdates()
                    state.value = BatchListState(emptyList(), PaginationStatus.None)
                }
            }
        }

        scope.launch {
            context.actionsFlow.collect { action ->
                collectActions(action)
            }
        }
    }

    private fun collectActions(action: BatchAction<TKey, TRequestParams, TUpdate>) {
        when (action) {
            is BatchAction.Reload -> {
                // Stop all tasks
                loadMoreActionJob?.cancel()
                reloadActionJob?.cancel()
                stopAllUpdates()
                reloadActionJob = scope.launch(fetchDispatcher) {
                    reloadTask(action)
                }
            }
            is BatchAction.LoadMore -> {
                if (loadMoreActionJob?.isActive == true) {
                    return
                }

                loadMoreActionJob = scope.launch(fetchDispatcher) {
                    reloadActionJob?.join()
                    loadMoreTask(action)
                }
            }
            is BatchAction.UpdateBatches -> {
                if (updateFetcher == null) return

                scope.launch(fetchDispatcher) {
                    // Lazily start a job so we can avoid batch update collisions
                    // by waiting for other tasks with the same keys to complete
                    val job = launch(start = CoroutineStart.LAZY) {
                        updateBatchesTask(action)
                    }

                    val actionJob = action to job

                    waitingUpdateJobs.update { it + actionJob }

                    // Wait for other update tasks that mutate batches with the same keys
                    updateJobs.first { workingJobs ->
                        action.keys.intersect(workingJobs.map { it.first.keys }.flatten().toSet()).isEmpty()
                    }

                    waitingUpdateJobs.update { it - actionJob }

                    // No other task are mutating batches with the same keys, so we can start a job
                    val started = job.start()

                    if (started) {
                        updateJobs.update { it + actionJob }

                        job.invokeOnCompletion { cause ->
                            // If the job was cancelled it is up to a canceller to remove job from the updateJobs list
                            if (cause !is CancellationException) {
                                updateJobs.update { it - actionJob }
                            }
                        }
                    }
                }
            }
            BatchAction.CancelAllUpdates -> {
                if (updateFetcher == null) return
                stopAllUpdates()
            }
            is BatchAction.CancelUpdates -> {
                if (updateFetcher == null) return
                stopUpdates(action.predicate)
            }
            BatchAction.CancelBatchLoading -> {
                loadMoreActionJob?.cancel()
                reloadActionJob?.cancel()
            }
        }
    }

    private suspend fun reloadTask(action: BatchAction.Reload<TRequestParams>) {
        state.value = BatchListState(
            data = emptyList(),
            status = PaginationStatus.InitialLoading,
        )

        val res = runCatching {
            batchFetcher.fetchFirst(action.requestParams)
        }.getOrElse { BatchFetchResult.Error(it) }

        state.value = when (res) {
            is BatchFetchResult.Success -> {
                val key = generateNewKey(listOf())
                val batch = Batch(
                    key = key,
                    data = res.data,
                )
                BatchListState(
                    data = listOf(batch),
                    status = if (res.last) {
                        PaginationStatus.EndOfPagination
                    } else {
                        PaginationStatus.Paginating(res)
                    },
                )
            }
            is BatchFetchResult.Error -> {
                BatchListState(
                    data = emptyList(),
                    status = PaginationStatus.InitialLoadingError(
                        throwable = res.throwable,
                    ),
                )
            }
        }

        lastRequestResult.value = res
    }

    private suspend fun loadMoreTask(action: BatchAction.LoadMore<TRequestParams>) {
        val status = state.value.status

        // Skip the action if the state is not ready to continue pagination.
        // Two options are acceptable:
        // 1. The Source is ready to load next page with the same or different request params.
        // 2. The Source has reached the end of pagination, but there is another request
        // that can possibly load the next page and continue the pagination

        if (status !is PaginationStatus.Paginating && status !is PaginationStatus.EndOfPagination) return
        if (status is PaginationStatus.EndOfPagination && action.requestParams == null) return

        val lastResult = lastRequestResult.value ?: return

        state.update { it.copy(status = PaginationStatus.NextBatchLoading) }

        val res = runCatching {
            batchFetcher.fetchNext(action.requestParams, lastResult)
        }.getOrElse { BatchFetchResult.Error(it) }

        lastRequestResult.value = lastResult

        state.update { currentState ->
            when (res) {
                is BatchFetchResult.Success -> {
                    val newBatch = Batch(
                        key = generateNewKey(currentState.data.map { it.key }),
                        data = res.data,
                    )

                    currentState.copy(
                        data = currentState.data + newBatch,
                        status = if (res.last) {
                            PaginationStatus.EndOfPagination
                        } else {
                            PaginationStatus.Paginating(res)
                        },
                    )
                }
                is BatchFetchResult.Error -> {
                    currentState.copy(
                        status = PaginationStatus.Paginating(res),
                    )
                }
            }
        }
    }

    private suspend fun updateBatchesTask(action: BatchAction.UpdateBatches<TKey, TUpdate>) {
        if (updateFetcher == null) return

        val batches = state.value.data
        val batchesToUpdate = batches.filter { action.keys.contains(it.key) }

        val result = updateFetcher.fetchUpdate(
            toUpdate = batchesToUpdate,
            updateRequest = action.updateRequest,
        )

        if (result is BatchUpdateResult.Success) {
            state.update { currentState ->
                val resMap = result.data.associateBy { it.key }
                currentState.copy(
                    data = currentState.data.map {
                        resMap[it.key] ?: it
                    },
                )
            }
        }

        updateResults.emit(action.updateRequest to result)
    }

    private fun stopAllUpdates() {
        updateJobs.update { actionJobs ->
            waitingUpdateJobs.update { waitingActionJobs ->
                waitingActionJobs.forEach {
                    it.second.cancel()
                }
                emptyList()
            }
            actionJobs.forEach {
                it.second.cancel()
            }
            emptyList()
        }
    }

    private fun stopUpdates(predicate: (BatchAction.UpdateBatches<TKey, TUpdate>) -> Boolean) {
        updateJobs.update { actionJobs ->
            waitingUpdateJobs.update { waitingActionJobs ->
                waitingActionJobs.mapNotNull {
                    if (predicate(it.first)) {
                        it.second.cancel()
                        null
                    } else {
                        it
                    }
                }
            }
            actionJobs.mapNotNull {
                if (predicate(it.first)) {
                    it.second.cancel()
                    null
                } else {
                    it
                }
            }
        }
    }
}