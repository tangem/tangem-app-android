package com.tangem.features.polymarket.impl.positions.model

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketPositionsFilter
import com.tangem.domain.polymarket.model.PolymarketPositionsState
import com.tangem.domain.polymarket.model.PolymarketPositionsTab
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.usecase.CreatePolymarketPositionsSessionUseCase
import com.tangem.features.polymarket.impl.positions.ui.state.PolymarketPositionsUM.Content
import com.tangem.features.polymarket.impl.positions.ui.state.PolymarketPositionsUM.FilteredContent
import com.tangem.features.polymarket.impl.positions.ui.state.PolymarketPositionsUM.NextPage
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class PolymarketPositionsLoader @Inject constructor(
    private val createSession: CreatePolymarketPositionsSessionUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    fun createFeed(
        userWalletId: UserWalletId,
        tab: PolymarketPositionsTab,
        onPositionClick: ((PolymarketPosition.Open) -> Unit)? = null,
        onClosedPositionClick: ((PolymarketPosition.Closed) -> Unit)? = null,
    ): Feed = Feed(
        userWalletId = userWalletId,
        tab = tab,
        onPositionClick = onPositionClick,
        onClosedPositionClick = onClosedPositionClick,
    )

    internal inner class Feed(
        userWalletId: UserWalletId,
        tab: PolymarketPositionsTab,
        onPositionClick: ((PolymarketPosition.Open) -> Unit)?,
        onClosedPositionClick: ((PolymarketPosition.Closed) -> Unit)?,
    ) {

        val state: StateFlow<FilteredContent>
            field = MutableStateFlow(FilteredContent())

        private val session = createSession(userWalletId, tab)
        private val converter = PolymarketPositionUMConverter(onPositionClick, onClosedPositionClick)
        private var observation: Job? = null
        private val selectedFilter = MutableStateFlow(PolymarketPositionsFilter.ALL)

        fun resume(scope: CoroutineScope) {
            if (observation == null) {
                observation = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    combine(session.state, selectedFilter) { value, filter -> value to filter }
                        .collectLatest { (value, filter) -> showState(value, filter) }
                }
            }
            session.resume(scope)
        }

        fun pause() {
            session.pause()
        }

        fun selectFilter(filter: PolymarketPositionsFilter) {
            if (filter == selectedFilter.value) return
            selectedFilter.value = filter
            session.selectFilter(filter)
        }

        fun dispose() {
            observation?.cancel()
            session.dispose()
        }

        private suspend fun showState(value: PolymarketPositionsState, filter: PolymarketPositionsFilter) {
            val content = if (value.filter != filter) {
                Content.Loading
            } else {
                when (val status = value.status) {
                    PaginationStatus.None, PaginationStatus.InitialLoading -> Content.Loading
                    is PaginationStatus.InitialLoadingError -> {
                        TangemLogger.e("Failed to load Polymarket positions: ${status.throwable}")
                        Content.Error(session::retry)
                    }
                    else -> convertData(value)
                }
            }
            if (filter == selectedFilter.value) state.value = FilteredContent(filter, content)
        }

        private suspend fun convertData(value: PolymarketPositionsState): Content {
            val pageError = (value.status as? PaginationStatus.Paginating)?.lastResult as? BatchFetchResult.Error
            if (pageError != null) {
                TangemLogger.e("Failed to load Polymarket positions page: ${pageError.throwable}")
            }
            if (value.positions.isEmpty()) {
                return when {
                    pageError != null -> Content.Error(session::retry)
                    value.status is PaginationStatus.NextBatchLoading -> Content.Loading
                    else -> Content.Empty
                }
            }
            return Content.Data(
                positions = withContext(dispatchers.default) {
                    value.positions.map(converter::convert).toImmutableList()
                },
                nextPage = when {
                    pageError != null -> NextPage.Error(session::retry)
                    value.status is PaginationStatus.NextBatchLoading -> NextPage.Loading
                    value.status is PaginationStatus.EndOfPagination -> NextPage.End
                    else -> NextPage.Ready(session::loadMore)
                },
            )
        }
    }
}