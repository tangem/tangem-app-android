package com.tangem.domain.polymarket.usecase

import com.tangem.domain.polymarket.PolymarketLiveScoresRepository
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketLiveScore
import com.tangem.domain.polymarket.model.PolymarketSportsState
import com.tangem.domain.polymarket.sports.PolymarketSportsMerger
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.isActive

class ObservePolymarketSportsStateUseCase(
    private val repository: PolymarketLiveScoresRepository,
    private val dispatchers: CoroutineDispatcherProvider,
    private val time: TimeSource = TimeSource.Monotonic,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(
        events: Flow<List<PolymarketEvent>>,
        isResumed: Flow<Boolean> = flowOf(true),
    ): Flow<PolymarketSportsState> = flow {
        val connections = repository.observeConnectionState()
        val merger = PolymarketSportsMerger(time)
        val gameIds = MutableStateFlow(emptySet<String>())
        merge(
            events.map(Update::Rest),
            isResumed.distinctUntilChanged().flatMapLatest { resumed ->
                if (resumed) liveUpdates(gameIds, connections) else emptyFlow()
            },
        ).collect { update ->
            when (update) {
                is Update.Rest -> gameIds.value = merger.setEvents(update.events)
                is Update.Score -> merger.update(update.value)
                Update.Connected -> merger.connected()
                Update.Tick -> Unit
            }
            emit(merger.snapshot())
        }
    }.distinctUntilChanged().conflate().flowOn(dispatchers.default)

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun liveUpdates(gameIds: StateFlow<Set<String>>, connections: Flow<Boolean>): Flow<Update> = merge(
        gameIds.flatMapLatest(repository::observeLiveScores).map(Update::Score),
        connections.filter { it }.map { Update.Connected },
        flow {
            while (currentCoroutineContext().isActive) {
                delay(1.seconds)
                emit(Update.Tick)
            }
        },
    )

    private sealed interface Update {
        data class Rest(val events: List<PolymarketEvent>) : Update
        data class Score(val value: PolymarketLiveScore) : Update
        data object Connected : Update
        data object Tick : Update
    }
}