package com.tangem.domain.polymarket.session

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketActivityRepository
import com.tangem.domain.polymarket.model.PolymarketActivity
import com.tangem.domain.polymarket.model.PolymarketActivityBatchAction
import com.tangem.domain.polymarket.model.PolymarketActivityBatchingContext
import com.tangem.domain.polymarket.model.PolymarketActivityFilter
import com.tangem.domain.polymarket.model.PolymarketActivityListConfig
import com.tangem.domain.polymarket.model.PolymarketActivityState
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns one wallet's history visit. Controls are called from the screen's scope. */
class PolymarketActivitySession internal constructor(
    private val userWalletId: UserWalletId,
    private val repository: PolymarketActivityRepository,
    private val deriveAddresses: DerivePolymarketAddressesUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    val state: StateFlow<PolymarketActivityState>
        field = MutableStateFlow(PolymarketActivityState())

    private var scope: CoroutineScope? = null
    private var loadingJob: Job? = null
    private var reconcileJob: Job? = null
    private var requestMore: (() -> Unit)? = null
    private var isDisposed = false

    fun resume(scope: CoroutineScope) {
        if (isDisposed || this.scope != null) return
        this.scope = scope
        load(shouldReconcile = true)
    }

    fun pause() {
        scope = null
        loadingJob?.cancel()
        reconcileJob?.cancel()
        requestMore = null
        state.update { it.copy(isRefreshing = false) }
    }

    fun dispose() {
        pause()
        isDisposed = true
    }

    fun selectFilter(filter: PolymarketActivityFilter) {
        if (isDisposed || state.value.filter == filter) return
        state.value = PolymarketActivityState(filter = filter)
        load()
    }

    fun loadMore() {
        if (state.value.isRefreshing || state.value.status !is PaginationStatus.Paginating) return
        requestMore?.invoke()
    }

    fun refresh() {
        if (state.value.isRefreshing || state.value.status == PaginationStatus.InitialLoading) return
        load(shouldReconcile = true)
    }

    fun retry() {
        if (state.value.status is PaginationStatus.InitialLoadingError) load() else loadMore()
    }

    private fun load(shouldReconcile: Boolean = false) {
        val scope = scope?.takeIf { it.isActive } ?: return
        loadingJob?.cancel()
        requestMore = null
        val previous = state.value
        state.value = previous.copy(
            status = PaginationStatus.InitialLoading,
            isRefreshing = previous.items.isNotEmpty(),
        )
        loadingJob = scope.launch {
            runSuspendCatching {
                val addresses = withContext(dispatchers.default) { deriveAddresses.stored(userWalletId) }
                ensureActive()
                checkNotNull(addresses) { "Stored addresses unavailable" }
                if (shouldReconcile) reconcile(addresses)
                collectPages(PolymarketActivityListConfig(addresses, previous.filter), previous.items)
            }.onFailure { error ->
                state.update { it.copy(status = PaginationStatus.InitialLoadingError(error), isRefreshing = false) }
            }
        }
    }

    private suspend fun collectPages(config: PolymarketActivityListConfig, previous: List<PolymarketActivity>) =
        coroutineScope {
            val actions = MutableSharedFlow<PolymarketActivityBatchAction>(replay = 1)
            val flow = repository.getActivityBatchFlow(
                context = PolymarketActivityBatchingContext(actions, this),
                batchSize = PAGE_SIZE,
            )
            var isPagePending = true
            var isReplacing = true
            requestMore = {
                if (isActive && !isPagePending && flow.state.value.status is PaginationStatus.Paginating) {
                    isPagePending = true
                    launch { actions.emit(BatchAction.LoadMore()) }
                }
            }
            launch {
                flow.state.collect { batch ->
                    val status = batch.status
                    if (status == PaginationStatus.None || status == PaginationStatus.InitialLoading) return@collect
                    isPagePending = status == PaginationStatus.NextBatchLoading
                    val items = withContext(dispatchers.default) {
                        batch.data.flatMap { it.data }.distinctBy { it.id }
                    }
                    val error = status.errorOrNull()
                    if (isReplacing && previous.isNotEmpty() && error != null) {
                        state.value = PolymarketActivityState(
                            items = previous,
                            filter = config.filter,
                            status = PaginationStatus.InitialLoadingError(error),
                        )
                        return@collect
                    }
                    val requiredCount = if (isReplacing) {
                        previous.size.coerceAtLeast(1)
                    } else {
                        state.value.items.size + 1
                    }
                    val hasNextPage = (status as? PaginationStatus.Paginating)?.lastResult is BatchFetchResult.Success
                    if (items.size < requiredCount && hasNextPage) {
                        requestMore?.invoke()
                        return@collect
                    }
                    if (isReplacing && status == PaginationStatus.NextBatchLoading) return@collect
                    isReplacing = false
                    state.value = PolymarketActivityState(items, config.filter, status)
                }
            }
            actions.emit(BatchAction.Reload(config))
        }

    private fun PaginationStatus<List<PolymarketActivity>>.errorOrNull(): Throwable? = when (this) {
        is PaginationStatus.InitialLoadingError -> throwable
        is PaginationStatus.Paginating -> (lastResult as? BatchFetchResult.Error)?.throwable
        else -> null
    }

    private fun reconcile(addresses: PolymarketAddresses) {
        val scope = scope ?: return
        if (reconcileJob?.isActive == true) return
        reconcileJob = scope.launch {
            repository.reconcileDeposits(addresses).fold(
                ifLeft = { TangemLogger.e("Failed to reconcile Polymarket deposits: $it") },
                ifRight = { retryAfter ->
                    delay(retryAfter)
                    state.first { !it.isRefreshing && it.status != PaginationStatus.InitialLoading }
                    // Re-read once after the suggested delay; 202 is not a signal to submit another scan.
                    load()
                },
            )
        }
    }

    private companion object {
        const val PAGE_SIZE = 30
    }
}