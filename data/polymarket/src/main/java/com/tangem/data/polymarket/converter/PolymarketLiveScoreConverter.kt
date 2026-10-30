package com.tangem.data.polymarket.converter

import com.tangem.domain.polymarket.model.PolymarketLiveScore
import com.tangem.grow.datasource.polymarket.socket.models.SportsResultDto
import com.tangem.utils.converter.Converter

internal object PolymarketLiveScoreConverter : Converter<PolymarketLiveScoreConverter.Input, PolymarketLiveScore> {

    override fun convert(value: Input): PolymarketLiveScore = PolymarketLiveScore(
        gameId = value.feedGameId,
        score = value.result.score,
        period = value.result.period,
        isLive = value.result.isLive,
        isEnded = value.result.isEnded,
    )

    /**
     * @property feedGameId the feed's spelling of [SportsResultDto.gameId], which may carry leading zeros
     */
    data class Input(val result: SportsResultDto, val feedGameId: String)
}