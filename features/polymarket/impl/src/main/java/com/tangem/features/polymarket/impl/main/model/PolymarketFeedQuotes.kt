package com.tangem.features.polymarket.impl.main.model

import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketLiveQuote
import com.tangem.domain.polymarket.model.PolymarketSportsState
import com.tangem.domain.polymarket.usecase.ObservePolymarketLiveQuotesUseCase
import com.tangem.features.polymarket.impl.common.isLiveForQuotes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.sample

internal class PolymarketFeedQuotes(
    private val observeQuotes: ObservePolymarketLiveQuotesUseCase,
    private val isEnabled: Boolean,
) {

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    fun observe(
        feed: PolymarketCategoryFeed,
        sports: StateFlow<PolymarketSportsState>,
        isResumed: StateFlow<Boolean>,
    ): Flow<Map<String, PolymarketLiveQuote>> {
        if (!isEnabled) return flowOf(emptyMap())
        return channelFlow {
            var displayed = emptyMap<String, PolymarketLiveQuote>()
            combine(
                flow = feed.batchFlow.state,
                flow2 = sports,
                flow3 = feed.visibleEventIds,
                flow4 = isResumed,
            ) { state, games, visible, resumed ->
                if (resumed) {
                    assetIds(
                        events = state.data.flatMap { it.data.events }.distinctBy(PolymarketEvent::id),
                        sports = games,
                        visible = visible,
                    )
                } else {
                    emptySet()
                }
            }.distinctUntilChanged().collectLatest { ids ->
                displayed = displayed.filterKeys { it in ids }
                send(displayed)
                if (ids.isNotEmpty()) {
                    observeQuotes(ids).sample(BATCH_INTERVAL_MILLIS).collect { quotes ->
                        displayed = quotes.filterKeys { it in ids }
                        send(displayed)
                    }
                }
            }
        }.distinctUntilChanged()
    }

    private fun assetIds(
        events: List<PolymarketEvent>,
        sports: PolymarketSportsState,
        visible: Set<String>,
    ): Set<String> {
        val first = events.indexOfFirst { it.id in visible }
        if (first < 0) return emptySet()
        val last = events.indexOfLast { it.id in visible }
        return events.subList(
            fromIndex = (first - PREFETCHED_NEIGHBOURS).coerceAtLeast(0),
            toIndex = (last + PREFETCHED_NEIGHBOURS + 1).coerceAtMost(events.size),
        ).asSequence()
            .filter { it.isLiveForQuotes(sports.events[it.id]) }
            .flatMap { it.markets.asSequence() }
            .flatMap { it.outcomes.asSequence() }
            .map { it.assetId }
            .filter(String::isNotBlank)
            .toSet()
    }

    private companion object {
        const val PREFETCHED_NEIGHBOURS = 5
        const val BATCH_INTERVAL_MILLIS = 300L
    }
}