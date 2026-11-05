package com.tangem.data.polymarket

import com.tangem.core.socket.SocketState
import com.tangem.data.polymarket.converter.PolymarketLiveScoreConverter
import com.tangem.domain.polymarket.PolymarketLiveScoresRepository
import com.tangem.domain.polymarket.model.PolymarketLiveScore
import com.tangem.grow.datasource.polymarket.socket.PolymarketSportsSocket
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal class DefaultPolymarketLiveScoresRepository @Inject constructor(
    private val sportsSocket: PolymarketSportsSocket,
    private val clock: PolymarketSportsClock,
    private val dispatchers: CoroutineDispatcherProvider,
) : PolymarketLiveScoresRepository {

    override fun observeLiveScores(gameIds: Set<String>): Flow<PolymarketLiveScore> {
        val feedGameIdsBySocketId = gameIds
            .mapNotNull { feedGameId -> feedGameId.toLongOrNull()?.let { socketId -> socketId to feedGameId } }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
        if (feedGameIdsBySocketId.isEmpty()) return emptyFlow()

        return flow {
            sportsSocket.results().collect { result ->
                val receivedAt = clock.now()
                feedGameIdsBySocketId[result.gameId]?.forEach { feedGameId ->
                    val score = PolymarketLiveScoreConverter.convert(
                        value = PolymarketLiveScoreConverter.Input(result, feedGameId, receivedAt),
                    )
                    emit(score)
                }
            }
        }.flowOn(dispatchers.default)
    }

    override fun observeConnectionState(): Flow<Boolean> = sportsSocket.state()
        .map { it == SocketState.Connected }
        .distinctUntilChanged()
}