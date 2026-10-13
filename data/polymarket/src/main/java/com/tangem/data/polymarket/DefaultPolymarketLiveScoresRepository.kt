package com.tangem.data.polymarket

import com.tangem.data.polymarket.converter.PolymarketLiveScoreConverter
import com.tangem.domain.polymarket.PolymarketLiveScoresRepository
import com.tangem.domain.polymarket.model.PolymarketLiveScore
import com.tangem.grow.datasource.polymarket.socket.PolymarketSportsSocket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transform
import javax.inject.Inject

internal class DefaultPolymarketLiveScoresRepository @Inject constructor(
    private val sportsSocket: PolymarketSportsSocket,
) : PolymarketLiveScoresRepository {

    override fun observeLiveScores(gameIds: Set<String>): Flow<PolymarketLiveScore> = flow {
        val feedGameIdsBySocketId = gameIds
            .mapNotNull { feedGameId -> feedGameId.toLongOrNull()?.let { socketId -> socketId to feedGameId } }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
        val lastScores = mutableMapOf<String, PolymarketLiveScore>()
        val scores = sportsSocket.results()
            .transform { result ->
                feedGameIdsBySocketId[result.gameId]?.forEach { feedGameId ->
                    emit(PolymarketLiveScoreConverter.convert(value = result, gameId = feedGameId))
                }
            }
            .filter { score -> lastScores.put(score.gameId, score) != score }
        emitAll(scores)
    }
}