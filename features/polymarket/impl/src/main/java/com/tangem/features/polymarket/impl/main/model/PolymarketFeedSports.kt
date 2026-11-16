package com.tangem.features.polymarket.impl.main.model

import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketSportsState
import com.tangem.domain.polymarket.usecase.ObservePolymarketSportsStateUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/** Each visible category owns one sports consumer; pausing releases its socket subscription. */
internal class PolymarketFeedSports(
    private val observeSports: ObservePolymarketSportsStateUseCase,
    private val isEnabled: Boolean,
) {

    fun observe(feed: PolymarketCategoryFeed, isResumed: StateFlow<Boolean>): Flow<PolymarketSportsState> {
        if (!isEnabled) return flowOf(PolymarketSportsState(emptyMap()))

        return flow {
            var lastRevision = 0L
            emitAll(
                observeSports(
                    feed.batchFlow.state.map { state ->
                        state.data.flatMap { it.data.events }.distinctBy(PolymarketEvent::id)
                    }.distinctUntilChanged(),
                    isResumed = isResumed,
                ).onEach { state ->
                    if (state.resnapshotRevision > lastRevision) {
                        lastRevision = state.resnapshotRevision
                        feed.poller.refreshLoadedPages()
                    }
                },
            )
        }
    }
}