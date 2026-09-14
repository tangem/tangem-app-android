package com.tangem.domain.polymarket.session

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.converter.PolymarketPositionStatusConverter
import com.tangem.domain.polymarket.model.PolymarketPositionsBatchAction
import com.tangem.domain.polymarket.model.PolymarketPositionsBatchingContext
import com.tangem.domain.polymarket.model.PolymarketPositionsListConfig
import com.tangem.domain.polymarket.model.PolymarketPositionsFilter
import com.tangem.domain.polymarket.model.PolymarketPositionsState
import com.tangem.domain.polymarket.model.PolymarketPositionsTab
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
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
    private val dispatchers: CoroutineDispatcherProvider,
) {

    val state: StateFlow<PolymarketPositionsState>
        field = MutableStateFlow(PolymarketPositionsState())

    private var scope: CoroutineScope? = null
    private var job: Job? = null
    private val request = MutableStateFlow(Request())
    private var isDisposed = false
    private var requestMore: (() -> Unit)? = null

    fun resume(scope: CoroutineScope) {
        if (isDisposed) return
        request.update { it.copy(isResumed = true) }
        this.scope = scope
        if (job == null) load()
    }

    fun pause() {
        request.update { it.copy(isResumed = false) }
    }

    fun dispose() {
        pause()
        isDisposed = true
        job?.cancel()
        requestMore = null
        scope = null
    }

    fun loadMore() {
        if (!request.value.isResumed || state.value.status !is PaginationStatus.Paginating) return
        if (state.value.filter != request.value.filter) return
        request.update { it.copy(targetCount = state.value.positions.size + PAGE_SIZE) }
        requestMore?.invoke()
    }

    fun selectFilter(filter: PolymarketPositionsFilter) {
        if (isDisposed || request.value.filter == filter) return
        request.update { it.copy(filter = filter, targetCount = PAGE_SIZE) }
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

    private fun load() {
        val scope = scope?.takeIf { it.isActive } ?: return
        job?.cancel()
        requestMore = null
        state.value = PolymarketPositionsState(
            status = PaginationStatus.InitialLoading,
            filter = request.value.filter,
        )
        job = scope.launch {
            val addresses = withContext(dispatchers.default) { deriveAddresses.stored(userWalletId) }
            ensureActive()
            if (addresses == null) {
                state.value = PolymarketPositionsState(
                    status = PaginationStatus.InitialLoadingError(
                        IllegalStateException("Stored addresses unavailable"),
                    ),
                    filter = request.value.filter,
                )
            } else {
                collectPages(PolymarketPositionsListConfig(addresses, tab))
            }
        }
    }

    private suspend fun collectPages(config: PolymarketPositionsListConfig) = coroutineScope {
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

        val batches = batchFlow.state.onEach { batch ->
            when (batch.status) {
                PaginationStatus.None, PaginationStatus.InitialLoading, PaginationStatus.NextBatchLoading -> Unit
                else -> isPagePending = false
            }
        }
        launch {
            combine(batches, request) { batch, selection -> batch to selection }.collectLatest { (batch, selection) ->
                if (batch.status == PaginationStatus.None) return@collectLatest
                val positions = withContext(dispatchers.default) {
                    batch.data.flatMap { it.data }.distinctBy { it.id }.filter { position ->
                        selection.filter == PolymarketPositionsFilter.ALL ||
                            PolymarketPositionStatusConverter.convert(position) == selection.filter.status
                    }
                }
                if (selection != request.value) return@collectLatest
                val shouldFindMatches = if (selection.filter == PolymarketPositionsFilter.ALL) {
                    positions.isEmpty()
                } else {
                    positions.size < selection.targetCount
                }
                val shouldLoadMore = shouldFindMatches &&
                    (batch.status as? PaginationStatus.Paginating)?.lastResult is BatchFetchResult.Success
                state.value = PolymarketPositionsState(
                    positions = positions,
                    status = if (shouldLoadMore) PaginationStatus.NextBatchLoading else batch.status,
                    filter = selection.filter,
                )
                if (shouldLoadMore) requestMore?.invoke()
            }
        }
        actions.emit(BatchAction.Reload(config))
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