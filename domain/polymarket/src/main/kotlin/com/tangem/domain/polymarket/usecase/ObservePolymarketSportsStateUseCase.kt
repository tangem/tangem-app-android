package com.tangem.domain.polymarket.usecase

import com.tangem.domain.polymarket.PolymarketLiveScoresRepository
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketLiveScore
import com.tangem.domain.polymarket.model.PolymarketSportsState
import com.tangem.domain.polymarket.sports.PolymarketSportsMerger
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

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
        val connections = repository.observeConnectionRevision()
        val merger = PolymarketSportsMerger(time, initialConnection = connections.value)
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
                is Update.Connection -> merger.connected(update.revision)
                Update.Tick -> Unit
            }
            emit(merger.snapshot())
        }
    }.distinctUntilChanged().conflate().flowOn(dispatchers.default)

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun liveUpdates(gameIds: StateFlow<Set<String>>, connections: StateFlow<Long>): Flow<Update> = merge(
        gameIds.flatMapLatest(repository::observeLiveScores).map(Update::Score),
        connections.map(Update::Connection),
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
        data class Connection(val revision: Long) : Update
        data object Tick : Update
    }
}