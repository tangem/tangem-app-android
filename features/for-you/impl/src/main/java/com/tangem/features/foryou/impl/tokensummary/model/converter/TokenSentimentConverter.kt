package com.tangem.features.foryou.impl.tokensummary.model.converter

import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
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
import java.math.BigDecimal

/**
 * Converts indicator readings of a coin into the token sentiment section state
 * for the selected [timeframe]
 *
 * @property timeframe timeframe selected in the period picker. Every indicator shows the reading
 * matching it, and has no state to show when the coin has no reading for that timeframe
 */
internal class TokenSentimentConverter(
    private val timeframe: CoinIndicators.Reading.Timeframe,
) : Converter<CoinIndicators, TokenSentimentUM> {

    override fun convert(value: CoinIndicators): TokenSentimentUM {
        // A row needs the name that comes with its reading, so a type with no reading for this timeframe has no row
        val readings = IndicatorType.entries.mapNotNull { indicatorType ->
            value.findReading(indicatorType.toReadingType(), timeframe)?.let { indicatorType to it }
        }
        val indicators = readings.map { (indicatorType, reading) -> buildIndicator(indicatorType, reading) }

        return if (readings.all { (_, reading) -> reading.value == null }) {
            TokenSentimentUM.Empty.NoOutlook(indicators = indicators.toImmutableList())
        } else {
            val totalScore = value.totalSentimentScore(timeframe)
            TokenSentimentUM.Content(
                sentiment = calculateSentiment(totalScore = totalScore),
                totalScore = totalScore,
                scaleMax = value.sentimentScaleMax(timeframe),
                lastUpdate = buildLastUpdate(readings),
                indicators = indicators.toImmutableList(),
            )
        }
    }

    private fun buildIndicator(indicatorType: IndicatorType, reading: CoinIndicators.Reading): TokenIndicatorUM.Loaded {
        val value = reading.value

        return if (value != null) {
            val (badgeText, badgeStatus) = reading.signal.toSentimentBadge()
            TokenIndicatorUM.Content(
                sentimentBadgeText = badgeText,
                sentimentBadgeStatus = badgeStatus,
                scoreBadgeText = stringReference(formatScore(type = reading.type, value = value)),
                indicatorType = indicatorType,
                title = reading.name,
            )
        } else {
            TokenIndicatorUM.NoData(indicatorType = indicatorType, title = reading.name)
        }
    }

    /**
     * MA Cross reads as the deviation of SMA50 from SMA200, so it shows a percent sign and keeps its direction;
     * the rest are plain numbers (RSI and the 0–100 social scores, MACD histogram).
     *
     * The backend sends the deviation already in percent units, while [percent] expects a fraction — hence the
     * `movePointLeft`, same as the markets converters do with the price-change percentages.
     */
    private fun formatScore(type: CoinIndicators.Reading.Type, value: BigDecimal): String {
        return when (type) {
            CoinIndicators.Reading.Type.MA_CROSS ->
                value.movePointLeft(2).format { percent(withoutSign = false) }
            CoinIndicators.Reading.Type.RSI,
            CoinIndicators.Reading.Type.MACD,
            CoinIndicators.Reading.Type.GALAXY_SCORE,
            CoinIndicators.Reading.Type.SENTIMENT,
            -> value.format { simple(decimals = 2) }
        }
    }

    /** The non-actionable signals share the neutral "none" badge */
    private fun CoinIndicators.Reading.Signal.toSentimentBadge(): Pair<TextReference, TangemBadge.Status> {
        return when (this) {
            CoinIndicators.Reading.Signal.POSITIVE ->
                resourceReference(R.string.common_positive) to TangemBadge.Status.Success
            CoinIndicators.Reading.Signal.NEGATIVE ->
                resourceReference(R.string.common_negative) to TangemBadge.Status.Error
            CoinIndicators.Reading.Signal.NEUTRAL ->
                resourceReference(R.string.common_neutral) to TangemBadge.Status.Info
            CoinIndicators.Reading.Signal.INSUFFICIENT_DATA,
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

    private fun buildLastUpdate(readings: List<Pair<IndicatorType, CoinIndicators.Reading>>): TextReference {
        val lastUpdatedAt = readings.mapNotNull { (_, reading) -> reading.updatedAt }.maxOrNull()
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