package com.tangem.domain.polymarket.session

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.model.PolymarketPositionsBatchAction
import com.tangem.domain.polymarket.model.PolymarketPositionsBatchingContext
import com.tangem.domain.polymarket.model.PolymarketPositionsListConfig
import com.tangem.domain.polymarket.model.PolymarketPositionsState
import com.tangem.domain.polymarket.model.PolymarketPositionsTab
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.pagination.BatchAction
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private var isResumed = false
    private var isDisposed = false
    private var requestMore: (() -> Unit)? = null

    fun resume(scope: CoroutineScope) {
        if (isDisposed) return
        isResumed = true
        this.scope = scope
        if (job == null) load()
    }

    fun pause() {
        isResumed = false
    }

    fun dispose() {
        pause()
        isDisposed = true
        job?.cancel()
        requestMore = null
        scope = null
    }

    fun loadMore() {
        requestMore?.invoke()
    }

    fun retry() {
        if (!isResumed) return
        if (state.value.status is PaginationStatus.InitialLoadingError) load() else loadMore()
    }

    private fun load() {
        val scope = scope?.takeIf { it.isActive } ?: return
        job?.cancel()
        requestMore = null
        state.value = PolymarketPositionsState(status = PaginationStatus.InitialLoading)
        job = scope.launch {
            val addresses = withContext(dispatchers.default) { deriveAddresses.stored(userWalletId) }
            ensureActive()
            if (addresses == null) {
                state.value = PolymarketPositionsState(
                    status = PaginationStatus.InitialLoadingError(
                        IllegalStateException("Stored addresses unavailable"),
                    ),
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
            if (isActive && isResumed && isPageReady) {
                isPagePending = true
                launch { actions.emit(BatchAction.LoadMore()) }
            }
        }

        launch(start = CoroutineStart.UNDISPATCHED) {
            batchFlow.state.collect { batch ->
                when (batch.status) {
                    PaginationStatus.None -> return@collect
                    PaginationStatus.InitialLoading, PaginationStatus.NextBatchLoading -> Unit
                    else -> isPagePending = false
                }
                state.value = withContext(dispatchers.default) {
                    PolymarketPositionsState(
                        positions = batch.data.flatMap { it.data }.distinctBy { it.id },
                        status = batch.status,
                    )
                }
            }
        }
        actions.emit(BatchAction.Reload(config))
    }

    private companion object {
        const val PAGE_SIZE = 30
    }
}