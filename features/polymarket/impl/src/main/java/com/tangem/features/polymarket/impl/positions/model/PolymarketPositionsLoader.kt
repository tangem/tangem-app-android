package com.tangem.features.polymarket.impl.positions.model

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.model.PolymarketPositionsBatchAction
import com.tangem.domain.polymarket.model.PolymarketPositionsBatchingContext
import com.tangem.domain.polymarket.model.PolymarketPositionsBatchListState
import com.tangem.domain.polymarket.model.PolymarketPositionsListConfig
import com.tangem.domain.polymarket.model.PolymarketPositionsTab
import com.tangem.domain.polymarket.model.PolymarketPositionId
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.features.polymarket.impl.positions.ui.state.PolymarketPositionsUM.Content
import com.tangem.features.polymarket.impl.positions.ui.state.PolymarketPositionsUM.NextPage
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.collections.immutable.toImmutableList
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
import javax.inject.Inject

internal class PolymarketPositionsLoader @Inject constructor(
    private val repository: PolymarketPositionsRepository,
    private val deriveAddresses: DerivePolymarketAddressesUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    fun createFeed(
        userWalletId: UserWalletId,
        tab: PolymarketPositionsTab,
        onPositionClick: ((PolymarketPositionId) -> Unit)? = null,
    ): Feed = Feed(userWalletId, tab, onPositionClick)

    internal inner class Feed(
        private val userWalletId: UserWalletId,
        private val tab: PolymarketPositionsTab,
        onPositionClick: ((PolymarketPositionId) -> Unit)?,
    ) {

        val state: StateFlow<Content>
            field = MutableStateFlow<Content>(Content.Loading)

        private val converter = PolymarketPositionUMConverter(onPositionClick)
        private var job: Job? = null
        private var isResumed = false

        fun resume(scope: CoroutineScope) {
            isResumed = true
            if (job == null) load(scope)
        }

        fun pause() {
            isResumed = false
        }

        fun dispose() {
            pause()
            job?.cancel()
        }

        private fun load(scope: CoroutineScope) {
            job?.cancel()
            state.value = Content.Loading
            job = scope.launch {
                val retry = { if (isResumed) load(scope) }
                val addresses = withContext(dispatchers.default) { deriveAddresses.stored(userWalletId) }
                ensureActive()
                if (addresses == null) {
                    showError("Stored addresses unavailable", retry)
                } else {
                    collectSession(PolymarketPositionsListConfig(addresses, tab), retry)
                }
            }
        }

        private suspend fun collectSession(config: PolymarketPositionsListConfig, retry: () -> Unit) = coroutineScope {
            val actions = MutableSharedFlow<PolymarketPositionsBatchAction>(replay = 1)
            val batchFlow = repository.getPositionsBatchFlow(
                context = PolymarketPositionsBatchingContext(actionsFlow = actions, coroutineScope = this),
                batchSize = PAGE_SIZE,
            )
            var isPagePending = true

            fun requestMore() {
                if (!canRequestPage(isPagePending, batchFlow.state.value)) return
                isPagePending = true
                launch { actions.emit(BatchAction.LoadMore()) }
            }

            launch(start = CoroutineStart.UNDISPATCHED) {
                batchFlow.state.collect { batch ->
                    when (val status = batch.status) {
                        PaginationStatus.None, PaginationStatus.InitialLoading -> return@collect
                        is PaginationStatus.InitialLoadingError -> {
                            isPagePending = false
                            showError(status.throwable.toString(), retry)
                            return@collect
                        }
                        PaginationStatus.NextBatchLoading -> Unit
                        else -> isPagePending = false
                    }

                    val pageError = (batch.status as? PaginationStatus.Paginating)
                        ?.lastResult as? BatchFetchResult.Error
                    if (pageError != null) {
                        TangemLogger.e("Failed to load Polymarket positions page: ${pageError.throwable}")
                    }
                    showBatch(batch, ::requestMore)
                }
            }
            actions.emit(BatchAction.Reload(config))
        }

        private fun CoroutineScope.canRequestPage(
            isPagePending: Boolean,
            batch: PolymarketPositionsBatchListState,
        ): Boolean = isActive && isResumed && !isPagePending && batch.status is PaginationStatus.Paginating

        private suspend fun showBatch(batch: PolymarketPositionsBatchListState, onLoadMore: () -> Unit) {
            val positions = withContext(dispatchers.default) {
                batch.data
                    .flatMap { it.data }
                    .distinctBy { it.id }
                    .map(converter::convert)
                    .toImmutableList()
            }
            val hasPageError = (batch.status as? PaginationStatus.Paginating)?.lastResult is BatchFetchResult.Error
            state.value = if (positions.isEmpty()) {
                Content.Empty
            } else {
                Content.Data(
                    positions = positions,
                    nextPage = when {
                        hasPageError -> NextPage.Error(onLoadMore)
                        batch.status is PaginationStatus.NextBatchLoading -> NextPage.Loading
                        batch.status is PaginationStatus.EndOfPagination -> NextPage.End
                        else -> NextPage.Ready(onLoadMore)
                    },
                )
            }
        }

        private fun showError(reason: String, retry: () -> Unit) {
            TangemLogger.e("Failed to refresh Polymarket positions: $reason")
            state.value = Content.Error(retry)
        }
    }

    private companion object {
        const val PAGE_SIZE = 30
    }
}