package com.tangem.data.polymarket

import com.tangem.data.polymarket.converter.PolymarketLiveScoreConverter
import com.tangem.domain.polymarket.PolymarketLiveScoresRepository
import com.tangem.domain.polymarket.model.PolymarketLiveScore
import com.tangem.grow.datasource.polymarket.socket.PolymarketSportsSocket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

internal class DefaultPolymarketLiveScoresRepository @Inject constructor(
    private val sportsSocket: PolymarketSportsSocket,
) : PolymarketLiveScoresRepository {

    override fun observeLiveScores(gameIds: Set<String>): Flow<PolymarketLiveScore> {
        val feedGameIdsBySocketId = gameIds
            .mapNotNull { feedGameId -> feedGameId.toLongOrNull()?.let { socketId -> socketId to feedGameId } }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
        if (feedGameIdsBySocketId.isEmpty()) return emptyFlow()

        return flow {
            val lastScores = mutableMapOf<String, PolymarketLiveScore>()
            sportsSocket.results().collect { result ->
                feedGameIdsBySocketId[result.gameId]?.forEach { feedGameId ->
                    val score = PolymarketLiveScoreConverter.convert(
                        value = PolymarketLiveScoreConverter.Input(result = result, feedGameId = feedGameId),
                    )
                    if (lastScores.put(feedGameId, score) != score) emit(score)
                }
            }
        }
    }
}