package com.tangem.domain.polymarket.session

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.converter.PolymarketPositionStatusConverter
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.flow.PredictionPositionsUpdateListener
import com.tangem.domain.polymarket.model.PolymarketPositionsBatchAction
import com.tangem.domain.polymarket.model.PolymarketPositionsBatchFlow
import com.tangem.domain.polymarket.model.PolymarketPositionsBatchingContext
import com.tangem.domain.polymarket.model.PolymarketPositionsListConfig
import com.tangem.domain.polymarket.model.PolymarketPositionsFilter
import com.tangem.domain.polymarket.model.PolymarketPositionsState
import com.tangem.domain.polymarket.model.PolymarketPositionsTab
import com.tangem.domain.polymarket.model.PolymarketPositionsUpdateRequest
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListState
import com.tangem.pagination.BatchUpdateResult
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One list visit. Pausing retains pages; disposal permanently ends the session. Call controls on the owning scope. */
class PolymarketPositionsSession internal constructor(
    private val userWalletId: UserWalletId,
    private val tab: PolymarketPositionsTab,
    private val repository: PolymarketPositionsRepository,
    private val deriveAddresses: DerivePolymarketAddressesUseCase,
    private val updateListener: PredictionPositionsUpdateListener,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    val state: StateFlow<PolymarketPositionsState>
        field = MutableStateFlow(PolymarketPositionsState())

    private var scope: CoroutineScope? = null
    private var job: Job? = null
    private var invalidationJob: Job? = null
    private var appliedRevision: Long = updateListener.currentRevision(userWalletId)
    private val request = MutableStateFlow(Request())
    private var isDisposed = false
    private var requestMore: (() -> Unit)? = null
    private var refreshLoadedPages: (() -> Unit)? = null
    private var cancelUpdates: (() -> Unit)? = null

    fun resume(scope: CoroutineScope) {
        if (isDisposed) return
        val wasResumed = request.value.isResumed
        request.update { it.copy(isResumed = true) }
        this.scope = scope
        val currentRevision = updateListener.currentRevision(userWalletId)
        val hasChangedSinceLoaded = currentRevision != appliedRevision
        appliedRevision = currentRevision
        when {
            state.value.refreshError != null -> load(isRefreshing = true)
            job == null || hasChangedSinceLoaded -> load()
            !wasResumed -> refreshLoadedPages?.invoke()
        }
        invalidationJob?.cancel()
        invalidationJob = scope.launch {
            updateListener.revisionFlow(userWalletId).collect { revision ->
                if (revision == appliedRevision) return@collect
                appliedRevision = revision
                load()
            }
        }
    }

    fun pause() {
        request.update { it.copy(isResumed = false) }
        invalidationJob?.cancel()
        invalidationJob = null
        cancelUpdates?.invoke()
        if (state.value.isRefreshing) {
            job?.cancel()
            job = null
            state.update { it.copy(isRefreshing = false) }
        }
    }

    fun dispose() {
        pause()
        isDisposed = true
        job?.cancel()
        requestMore = null
        refreshLoadedPages = null
        cancelUpdates = null
        scope = null
    }

    fun loadMore() {
        if (state.value.isRefreshing || state.value.refreshError != null) return
        if (!request.value.isResumed || state.value.status !is PaginationStatus.Paginating) return
        if (state.value.filter != request.value.filter) return
        request.update { it.copy(targetCount = state.value.positions.size + PAGE_SIZE) }
        requestMore?.invoke()
    }

    fun selectFilter(filter: PolymarketPositionsFilter) {
        if (isDisposed || request.value.filter == filter) return
        request.update { it.copy(filter = filter, targetCount = PAGE_SIZE) }
        if (state.value.refreshError != null || state.value.isRefreshing) {
            if (request.value.isResumed) load(isRefreshing = state.value.isRefreshing)
            return
        }
        if (requestMore == null) state.update { it.copy(filter = filter) }
    }

    fun retry() {
        if (!request.value.isResumed) return
        when (val status = state.value.status) {
            is PaginationStatus.InitialLoadingError -> load()
            is PaginationStatus.Paginating -> if (status.lastResult is BatchFetchResult.Error) requestMore?.invoke()
            else -> Unit
        }
    }

    fun refresh() {
        if (isDisposed || !request.value.isResumed || state.value.isRefreshing) return
        request.update { it.copy(targetCount = PAGE_SIZE) }
        load(isRefreshing = true)
    }

    private fun load(isRefreshing: Boolean = false) {
        val scope = scope?.takeIf { it.isActive } ?: return
        val retained = state.value.takeIf { current ->
            isRefreshing && current.filter == request.value.filter &&
                (current.status is PaginationStatus.Paginating ||
                    current.status is PaginationStatus.EndOfPagination ||
                    current.positions.isNotEmpty())
        }
        job?.cancel()
        requestMore = null
        refreshLoadedPages = null
        cancelUpdates = null
        state.value = retained?.copy(isRefreshing = true, refreshError = null) ?: PolymarketPositionsState(
            status = PaginationStatus.InitialLoading,
            filter = request.value.filter,
            isRefreshing = isRefreshing,
        )
        job = scope.launch {
            val addresses = withContext(dispatchers.default) { deriveAddresses.stored(userWalletId) }
            ensureActive()
            if (addresses == null) {
                val error = IllegalStateException("Stored addresses unavailable")
                state.value = retained?.copy(isRefreshing = false, refreshError = error) ?: PolymarketPositionsState(
                    status = PaginationStatus.InitialLoadingError(error),
                    filter = request.value.filter,
                )
            } else {
                collectPages(PolymarketPositionsListConfig(addresses, tab), retained)
            }
        }
    }

    private suspend fun collectPages(config: PolymarketPositionsListConfig, retained: PolymarketPositionsState?) {
        coroutineScope {
            val actions = MutableSharedFlow<PolymarketPositionsBatchAction>(replay = 1)
            val batchFlow = repository.getPositionsBatchFlow(
                context = PolymarketPositionsBatchingContext(actionsFlow = actions, coroutineScope = this),
                batchSize = PAGE_SIZE,
            )
            var isPagePending = true
            requestMore = {
                val isPageReady = !isPagePending && batchFlow.state.value.status is PaginationStatus.Paginating
                if (isActive && request.value.isResumed && isPageReady) {
                    isPagePending = true
                    launch { actions.emit(BatchAction.LoadMore()) }
                }
            }

            refreshLoadedPages = pageRefresher(batchFlow, actions, config)
            cancelUpdates = { launch { actions.emit(BatchAction.CancelAllUpdates) } }
            logRefreshFailures(batchFlow)

            val batches = batchFlow.state.onEach { batch ->
                when (batch.status) {
                    PaginationStatus.None, PaginationStatus.InitialLoading, PaginationStatus.NextBatchLoading -> Unit
                    else -> isPagePending = false
                }
            }
            launch {
                combine(
                    batches,
                    request,
                ) { batch, selection -> batch to selection }.collectLatest { (batch, selection) ->
                    showBatch(batch, selection, retained)
                }
            }
            actions.emit(BatchAction.Reload(config))
        }
    }

    private suspend fun showBatch(
        batch: BatchListState<Int, List<PolymarketPosition>>,
        selection: Request,
        retained: PolymarketPositionsState?,
    ) {
        if (batch.status == PaginationStatus.None || state.value.refreshError != null) return
        val positions = withContext(dispatchers.default) {
            batch.data.flatMap { it.data }.distinctBy { it.id }.filter { position ->
                selection.filter == PolymarketPositionsFilter.ALL ||
                    PolymarketPositionStatusConverter.convert(position) == selection.filter.status
            }
        }
        if (selection != request.value) return
        val shouldFindMatches = if (selection.filter == PolymarketPositionsFilter.ALL) {
            positions.isEmpty()
        } else {
            positions.size < selection.targetCount
        }
        val shouldLoadMore = shouldFindMatches &&
            (batch.status as? PaginationStatus.Paginating)?.lastResult is BatchFetchResult.Success
        val isLoading = shouldLoadMore || batch.status is PaginationStatus.InitialLoading ||
            batch.status is PaginationStatus.NextBatchLoading
        val next = PolymarketPositionsState(
            positions = positions,
            status = if (shouldLoadMore) PaginationStatus.NextBatchLoading else batch.status,
            filter = selection.filter,
            isRefreshing = state.value.isRefreshing && isLoading,
        )
        state.value = applyRefreshState(next, retained)
        if (shouldLoadMore) requestMore?.invoke()
    }

    private fun applyRefreshState(
        next: PolymarketPositionsState,
        retained: PolymarketPositionsState?,
    ): PolymarketPositionsState {
        if (!state.value.isRefreshing || retained == null) return next
        val error = when (val status = next.status) {
            is PaginationStatus.InitialLoadingError -> status.throwable
            is PaginationStatus.Paginating -> (status.lastResult as? BatchFetchResult.Error)?.throwable
            else -> null
        }
        return when {
            error != null -> retained.copy(isRefreshing = false, refreshError = error)
            next.isRefreshing -> state.value
            else -> next
        }
    }

    private fun CoroutineScope.pageRefresher(
        batchFlow: PolymarketPositionsBatchFlow,
        actions: MutableSharedFlow<PolymarketPositionsBatchAction>,
        config: PolymarketPositionsListConfig,
    ): () -> Unit = {
        val keys = batchFlow.state.value.data.mapTo(mutableSetOf()) { it.key }
        if (isActive && keys.isEmpty()) {
            load()
        } else if (isActive) {
            launch {
                actions.emit(BatchAction.CancelAllUpdates)
                actions.emit(
                    BatchAction.UpdateBatches(
                        keys = keys,
                        updateRequest = PolymarketPositionsUpdateRequest(config),
                        async = true,
                    ),
                )
            }
        }
    }

    private fun CoroutineScope.logRefreshFailures(batchFlow: PolymarketPositionsBatchFlow) {
        batchFlow.updateResults
            .onEach { (_, result) ->
                if (result is BatchUpdateResult.Error) {
                    TangemLogger.e("Failed to refresh Polymarket positions: ${result.throwable}")
                }
            }
            .launchIn(this)
    }

    private companion object {
        const val PAGE_SIZE = 30
    }

    private data class Request(
        val filter: PolymarketPositionsFilter = PolymarketPositionsFilter.ALL,
        val targetCount: Int = PAGE_SIZE,
        val isResumed: Boolean = false,
    )
}