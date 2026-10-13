package com.tangem.data.polymarket

import com.tangem.core.socket.SocketState
import com.tangem.data.polymarket.converter.PolymarketLiveScoreConverter
import com.tangem.domain.polymarket.PolymarketLiveScoresRepository
import com.tangem.domain.polymarket.model.PolymarketLiveScore
import com.tangem.grow.datasource.polymarket.socket.PolymarketSportsSocket
import com.tangem.grow.datasource.polymarket.socket.models.SportsResultDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import javax.inject.Inject

internal class DefaultPolymarketLiveScoresRepository @Inject constructor(
    private val sportsSocket: PolymarketSportsSocket,
) : PolymarketLiveScoresRepository {

    override fun observeLiveScores(gameIds: Set<String>): Flow<PolymarketLiveScore> {
        val feedGameIdsBySocketId = gameIds
            .mapNotNull { feedGameId -> feedGameId.toLongOrNull()?.let { socketId -> socketId to feedGameId } }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
        if (feedGameIdsBySocketId.isEmpty()) return emptyFlow()

        val reconnects = sportsSocket.state().filter { it == SocketState.Connected }.map { null }
        return flow {
            val lastScores = mutableMapOf<String, PolymarketLiveScore>()
            merge(reconnects, sportsSocket.results()).collect { result: SportsResultDto? ->
                if (result == null) {
                    lastScores.clear()
                    return@collect
                }
                feedGameIdsBySocketId[result.gameId]?.forEach { feedGameId ->
                    val score = PolymarketLiveScoreConverter.convert(value = result, gameId = feedGameId)
                    if (lastScores.put(feedGameId, score) != score) emit(score)
                }
            }
        }
    }
}