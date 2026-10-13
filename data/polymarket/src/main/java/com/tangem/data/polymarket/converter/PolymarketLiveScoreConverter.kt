package com.tangem.data.polymarket.converter

import com.tangem.domain.polymarket.model.PolymarketLiveScore
import com.tangem.grow.datasource.polymarket.socket.models.SportsResultDto

internal object PolymarketLiveScoreConverter {

    fun convert(value: SportsResultDto): PolymarketLiveScore = PolymarketLiveScore(
        gameId = value.gameId.toString(),
        score = value.score,
        period = value.period,
        isLive = value.isLive,
        isEnded = value.isEnded,
    )
}