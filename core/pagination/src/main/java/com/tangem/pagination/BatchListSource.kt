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
 * @param TError Type of the error that can occur during fetching or updating.
 * @property state State of the paginated data.
 * @property updateResults Flow of results of update requests.
 */
interface BatchListSource<TKey, TData, TUpdate, TError> {
    val state: StateFlow<BatchListState<TKey, TData, TError>>
    val updateResults: SharedFlow<Pair<TUpdate, BatchFetchUpdateResult<TKey, TData, TError>>>
}

/**
 * Creates a new [BatchListSource] with the provided configuration.
 *
 * @param config Configuration for batching.
 * @param context Context for batching.
 * @param generateNewKey Function to generate a new key for a batch.
 * @param batchFetcher Function to fetch a batch of data.
 *
 * @return New instance of [BatchListSource].
 */
@Suppress("FunctionNaming")
fun <TKey, TData, TRequest : Any, TError> BatchListSource(
    context: BatchingContext<TRequest, TKey, Nothing>,
    generateNewKey: suspend (List<TKey>) -> TKey,
    batchFetcher: BatchFetcher<TRequest, TData, TError>,
): BatchListSource<TKey, TData, Nothing, TError> = BatchListSourceImpl(context, generateNewKey, batchFetcher, null)

/**
 * Creates a new [BatchListSource] with the provided configuration.
 *
 * @param config Configuration for batching.
 * @param context Context for batching.
 * @param generateNewKey Function to generate a new key for a batch.
 * @param batchFetcher Function to fetch a batch of data.
 * @param updateFetcher Function to fetch updates for batches.
 *
 * @return New instance of [BatchListSource].
 */
@Suppress("FunctionNaming")
fun <TKey, TData, TUpdate, TRequest : Any, TError> BatchListSource(
    context: BatchingContext<TRequest, TKey, TUpdate>,
    generateNewKey: suspend (List<TKey>) -> TKey,
    batchFetcher: BatchFetcher<TRequest, TData, TError>,
    updateFetcher: BatchUpdateFetcher<TKey, TData, TError, TUpdate>,
): BatchListSource<TKey, TData, TUpdate, TError> =
    BatchListSourceImpl(context, generateNewKey, batchFetcher, updateFetcher)

private class BatchListSourceImpl<TKey, TData, TUpdate, TRequest : Any, TError>(
    private val context: BatchingContext<TRequest, TKey, TUpdate>,
    private val generateNewKey: suspend (List<TKey>) -> TKey,
    private val batchFetcher: BatchFetcher<TRequest, TData, TError>,
    private val updateFetcher: BatchUpdateFetcher<TKey, TData, TError, TUpdate>? = null,
) : BatchListSource<TKey, TData, TUpdate, TError> {

    override val state = MutableStateFlow(BatchListState<TKey, TData, TError>(emptyList(), PaginationStatus.None))
    override val updateResults = MutableSharedFlow<Pair<TUpdate, BatchFetchUpdateResult<TKey, TData, TError>>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val scope = context.coroutineScope
    private val updateJobs = MutableStateFlow<List<Pair<BatchAction.UpdateBatches<TKey, TUpdate>, Job>>>(emptyList())
    private val waitingUpdateJobs =
        MutableStateFlow<List<Pair<BatchAction.UpdateBatches<TKey, TUpdate>, Job>>>(emptyList())

    private val lastRequestResult = MutableStateFlow<BatchFetchResult<TData, TError>?>(null)
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

    private fun collectActions(action: BatchAction<TRequest, TKey, TUpdate>) {
        when (action) {
            is BatchAction.Reload -> {
                // Stop all tasks
                loadMoreActionJob?.cancel()
                reloadActionJob?.cancel()
                stopAllUpdates()
                reloadActionJob = scope.launch(Dispatchers.IO) {
                    reloadTask(action)
                }
            }
            is BatchAction.LoadMore -> {
                if (loadMoreActionJob?.isActive == true) {
                    return
                }

                loadMoreActionJob = scope.launch(Dispatchers.IO) {
                    reloadActionJob?.join()
                    loadMoreTask(action)
                }
            }
            is BatchAction.UpdateBatches -> {
                if (updateFetcher == null) return

                scope.launch(Dispatchers.IO) {
                    val job = launch(start = CoroutineStart.LAZY) {
                        updateBatchesTask(action)
                    }

                    val actionJob = action to job

                    waitingUpdateJobs.update { it + actionJob }

                    updateJobs.first { workingJobs ->
                        action.keys.intersect(workingJobs.map { it.first.keys }.flatten().toSet()).isEmpty()
                    }

                    waitingUpdateJobs.update { it - actionJob }

                    val started = job.start()

                    if (started) {
                        updateJobs.update { it + actionJob }

                        job.invokeOnCompletion { cause ->
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

    private suspend fun reloadTask(action: BatchAction.Reload<TRequest>) {
        state.value = BatchListState(
            data = emptyList(),
            status = PaginationStatus.InitialLoading,
        )

        val res = runCatching {
            batchFetcher.fetchFirst(action.request)
        }.getOrElse { BatchFetchResult.UnknownError(it) }

        state.value = if (res is BatchFetchResult.Success) {
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
        } else {
            BatchListState(
                data = emptyList(),
                status = PaginationStatus.InitialLoadingError(
                    error = (res as? BatchFetchResult.Error)?.error,
                ),
            )
        }

        lastRequestResult.value = res
    }

    private suspend fun loadMoreTask(action: BatchAction.LoadMore<TRequest>) {
        val status = state.value.status

        if (status !is PaginationStatus.Paginating && status !is PaginationStatus.EndOfPagination) return
        if (status is PaginationStatus.EndOfPagination && action.request == null) return

        val lastResult = lastRequestResult.value ?: return

        state.update { it.copy(status = PaginationStatus.NextBatchLoading) }

        val res = runCatching {
            batchFetcher.fetchNext(action.request, lastResult)
        }.getOrElse { BatchFetchResult.UnknownError(it) }

        lastRequestResult.value = lastResult

        state.update { currentState ->
            if (res is BatchFetchResult.Success) {
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
            } else {
                currentState.copy(
                    status = PaginationStatus.Paginating(res),
                )
            }
        }
    }

    private suspend fun updateBatchesTask(action: BatchAction.UpdateBatches<TKey, TUpdate>) {
        if (updateFetcher == null) return

        val batches = state.value.data
        val batchesToUpdate = batches.filter { action.keys.contains(it.key) }

        val result = updateFetcher.fetchUpdate(
            toUpdate = batchesToUpdate,
            updateRequest = action.request,
        )

        if (result is BatchFetchUpdateResult.Success) {
            state.update { currentState ->
                val resMap = result.data.associateBy { it.key }
                currentState.copy(
                    data = currentState.data.map {
                        resMap[it.key] ?: it
                    },
                )
            }
        }

        updateResults.emit(action.request to result)
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