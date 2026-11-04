package com.tangem.features.polymarket.impl.main.model

import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketSportsState
import com.tangem.domain.polymarket.usecase.ObservePolymarketSportsStateUseCase
import com.tangem.pagination.PaginationStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** One selection owns the score history, even when refresh replaces its pages; pausing releases the socket. */
internal class PolymarketFeedSports(
    private val observeSports: ObservePolymarketSportsStateUseCase,
    private val isEnabled: Boolean,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(feeds: Flow<PolymarketCategoryFeed>, isResumed: StateFlow<Boolean>): Flow<PolymarketSportsState> {
        if (!isEnabled) return flowOf(PolymarketSportsState(emptyMap()))

        return flow {
            var lastRevision = 0L
            val events = feeds.flatMapLatest { it.batchFlow.state }
                // Loading/disposal is not an empty REST response and must not discard remembered final scores.
                .filter { state ->
                    state.status is PaginationStatus.Paginating ||
                        state.status is PaginationStatus.NextBatchLoading ||
                        state.status is PaginationStatus.EndOfPagination
                }
                .map { state -> state.data.flatMap { it.data.events }.distinctBy(PolymarketEvent::id) }
                .distinctUntilChanged()
            emitAll(
                combine(observeSports(events, isResumed), feeds) { state, feed ->
                    if (state.resnapshotRevision > lastRevision) {
                        lastRevision = state.resnapshotRevision
                        feed.poller.refreshLoadedPages()
                    }
                    state
                }.distinctUntilChanged(),
            )
        }
    }
}