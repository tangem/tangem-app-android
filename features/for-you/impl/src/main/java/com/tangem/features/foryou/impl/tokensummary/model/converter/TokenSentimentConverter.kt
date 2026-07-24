package com.tangem.features.foryou.impl.tokensummary.model.converter

import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.simple
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.core.ui.utils.formatAsDateTime
import com.tangem.domain.markets.CoinIndicators
import com.tangem.domain.markets.findReading
import com.tangem.domain.markets.sentimentScaleMax
import com.tangem.domain.markets.totalSentimentScore
import com.tangem.features.foryou.impl.R
import com.tangem.features.foryou.impl.tokensummary.entity.IndicatorType
import com.tangem.features.foryou.impl.tokensummary.entity.TokenIndicatorUM
import com.tangem.features.foryou.impl.tokensummary.entity.TokenSentimentUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList

/**
 * Converts indicator readings of a coin into the token sentiment section state
 * for the selected [timeframe]
 *
 * @property timeframe timeframe selected in the period picker. RSI and MACD show the reading
 * matching it; the timeframe-agnostic indicators (MA Cross, Galaxy score, Sentiment) always
 * show their single reading
 */
internal class TokenSentimentConverter(
    private val timeframe: CoinIndicators.Reading.Timeframe,
) : Converter<CoinIndicators, TokenSentimentUM> {

    override fun convert(value: CoinIndicators): TokenSentimentUM {
        val readings = IndicatorType.entries.map { indicatorType ->
            indicatorType to value.findReading(indicatorType.toReadingType(), timeframe)
        }

        return if (readings.all { it.second?.value == null }) {
            TokenSentimentUM.Empty(resourceReference(R.string.token_summary_outlook_is_not_available))
        } else {
            val totalScore = value.totalSentimentScore(timeframe)
            TokenSentimentUM.Content(
                sentiment = calculateSentiment(totalScore = totalScore),
                totalScore = totalScore,
                scaleMax = value.sentimentScaleMax(timeframe),
                lastUpdate = buildLastUpdate(readings),
                indicators = readings
                    .map { (indicatorType, reading) -> buildIndicator(indicatorType, reading) }
                    .toImmutableList(),
            )
        }
    }

    private fun buildIndicator(indicatorType: IndicatorType, reading: CoinIndicators.Reading?): TokenIndicatorUM {
        return if (reading != null && reading.value != null) {
            val (badgeText, badgeStatus) = reading.signal.toSentimentBadge()
            TokenIndicatorUM.Content(
                sentimentBadgeText = badgeText,
                sentimentBadgeStatus = badgeStatus,
                scoreBadgeText = stringReference(reading.value.format { simple(decimals = 2) }),
                indicatorType = indicatorType,
            )
        } else {
            TokenIndicatorUM.NoData(indicatorType = indicatorType)
        }
    }

    /** `null` for the non-signal states — the indicator row falls back to [TokenIndicatorUM.NoData] */
    private fun CoinIndicators.Reading.Signal.toSentimentBadge(): Pair<TextReference, TangemBadge.Status> {
        return when (this) {
            CoinIndicators.Reading.Signal.BULLISH ->
                resourceReference(R.string.common_positive) to TangemBadge.Status.Success
            CoinIndicators.Reading.Signal.BEARISH ->
                resourceReference(R.string.common_negative) to TangemBadge.Status.Error
            CoinIndicators.Reading.Signal.NEUTRAL ->
                resourceReference(R.string.common_neutral) to TangemBadge.Status.Info
            CoinIndicators.Reading.Signal.INSUFFICIENT_DATA,
            CoinIndicators.Reading.Signal.NOT_APPLICABLE,
            CoinIndicators.Reading.Signal.NOT_AVAILABLE,
            -> resourceReference(R.string.common_none) to TangemBadge.Status.Neutral
        }
    }

    private fun calculateSentiment(totalScore: Int): TextReference {
        return resourceReference(
            when {
                totalScore > 0 -> R.string.token_summary_positive_outlook_title
                totalScore < 0 -> R.string.token_summary_negative_outlook_title
                else -> R.string.token_summary_neutral_outlook_title
            },
        )
    }

    private fun buildLastUpdate(readings: List<Pair<IndicatorType, CoinIndicators.Reading?>>): TextReference {
        val lastUpdatedAt = readings.mapNotNull { (_, reading) -> reading?.updatedAt }.maxOrNull()
            ?: return TextReference.EMPTY

        return resourceReference(
            R.string.token_summary_last_update_subtitle,
            wrappedList(lastUpdatedAt.millis.formatAsDateTime(DateTimeFormatters.dateDDMMYYYY)),
        )
    }

    private fun IndicatorType.toReadingType(): CoinIndicators.Reading.Type {
        return when (this) {
            IndicatorType.GalaxyScore -> CoinIndicators.Reading.Type.GALAXY_SCORE
            IndicatorType.Sentiment -> CoinIndicators.Reading.Type.SENTIMENT
            IndicatorType.RSI -> CoinIndicators.Reading.Type.RSI
            IndicatorType.MACD -> CoinIndicators.Reading.Type.MACD
            IndicatorType.MA_CROSS -> CoinIndicators.Reading.Type.MA_CROSS
        }
    }
}