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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

internal class DefaultPolymarketLiveScoresRepository @Inject constructor(
    private val sportsSocket: PolymarketSportsSocket,
) : PolymarketLiveScoresRepository {

    override fun observeLiveScores(gameIds: Set<String>): Flow<PolymarketLiveScore> = flow {
        val stats = PolymarketSportsFeedStats(trackedGameIds = gameIds)
        val lastScores = mutableMapOf<String, PolymarketLiveScore>()
        val scores = sportsSocket.results()
            .onStart { TangemLogger.i("Sports socket: observing ${gameIds.size} games $gameIds") }
            .onEach(stats::record)
            .onCompletion { stats.logStopped() }
            .filter { result -> result.gameId.toString() in gameIds }
            .map(PolymarketLiveScoreConverter::convert)
            .filter { score -> lastScores.put(score.gameId, score) != score }
        emitAll(scores)
    }
}