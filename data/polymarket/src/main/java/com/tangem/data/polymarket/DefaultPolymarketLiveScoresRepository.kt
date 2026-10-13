package com.tangem.data.polymarket

import com.tangem.data.polymarket.converter.PolymarketLiveScoreConverter
import com.tangem.domain.polymarket.PolymarketLiveScoresRepository
import com.tangem.domain.polymarket.model.PolymarketLiveScore
import com.tangem.grow.datasource.polymarket.socket.PolymarketSportsSocket
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform
import javax.inject.Inject

internal class DefaultPolymarketLiveScoresRepository @Inject constructor(
    private val sportsSocket: PolymarketSportsSocket,
) : PolymarketLiveScoresRepository {

    override fun observeLiveScores(gameIds: Set<String>): Flow<PolymarketLiveScore> = flow {
        val feedGameIdsBySocketId = gameIds
            .mapNotNull { feedGameId -> feedGameId.toLongOrNull()?.let { socketId -> socketId to feedGameId } }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
        val stats = PolymarketSportsFeedStats(trackedGameIds = feedGameIdsBySocketId.keys)
        val lastScores = mutableMapOf<String, PolymarketLiveScore>()
        val scores = sportsSocket.results()
            .onStart { TangemLogger.i("Sports socket: observing ${gameIds.size} games $gameIds") }
            .onEach(stats::record)
            .onCompletion { stats.logStopped() }
            .transform { result ->
                feedGameIdsBySocketId[result.gameId].orEmpty().forEach { feedGameId ->
                    emit(PolymarketLiveScoreConverter.convert(value = result, gameId = feedGameId))
                }
            }
            .filter { score -> lastScores.put(score.gameId, score) != score }
        emitAll(scores)
    }
}