package com.tangem.data.polymarket.converter

import com.tangem.domain.polymarket.model.PolymarketLiveScore
import com.tangem.grow.datasource.polymarket.socket.models.SportsResultDto
import com.tangem.utils.converter.Converter
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.time.Instant

internal object PolymarketLiveScoreConverter : Converter<PolymarketLiveScoreConverter.Input, PolymarketLiveScore> {

    private const val MILLIS_THRESHOLD = 100_000_000_000L

    override fun convert(value: Input): PolymarketLiveScore = PolymarketLiveScore(
        gameId = value.feedGameId,
        score = value.result.score,
        period = value.result.period,
        isLive = value.result.isLive,
        isEnded = value.result.isEnded,
        homeTeam = value.result.homeTeam,
        awayTeam = value.result.awayTeam,
        updatedAt = timestamp(value.result.lastUpdate as? JsonPrimitive) ?: value.receivedAt,
        lastSeenAt = value.receivedAt,
    )

    /**
     * @property feedGameId the feed's spelling of [SportsResultDto.gameId], which may carry leading zeros
     */
    data class Input(val result: SportsResultDto, val feedGameId: String, val receivedAt: Instant)

    private fun timestamp(value: JsonPrimitive?): Instant? {
        val raw = value?.contentOrNull ?: return null
        return runCatching {
            val number = raw.toLongOrNull()
            when {
                number == null -> Instant.parse(raw)
                number < 0 -> null
                number < MILLIS_THRESHOLD -> Instant.ofEpochSecond(number)
                else -> Instant.ofEpochMilli(number)
            }
        }.getOrNull()
    }
}