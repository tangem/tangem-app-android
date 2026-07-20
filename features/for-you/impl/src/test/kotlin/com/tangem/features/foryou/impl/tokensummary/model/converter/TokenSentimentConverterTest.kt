package com.tangem.features.foryou.impl.tokensummary.model.converter

import androidx.annotation.StringRes
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds.badge.TangemBadgeColor
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.core.ui.utils.toDateFormatWithTodayYesterday
import com.tangem.domain.markets.CoinIndicators
import com.tangem.domain.markets.CoinIndicators.Reading.Signal
import com.tangem.domain.markets.CoinIndicators.Reading.Timeframe
import com.tangem.domain.markets.CoinIndicators.Reading.Type
import com.tangem.features.foryou.impl.R
import com.tangem.features.foryou.impl.tokensummary.entity.IndicatorType
import com.tangem.features.foryou.impl.tokensummary.entity.TokenIndicatorUM
import com.tangem.features.foryou.impl.tokensummary.entity.TokenSentimentUM
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TokenSentimentConverterTest {

    private lateinit var defaultLocale: Locale

    @BeforeAll
    fun setUp() {
        defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)

        mockkObject(DateTimeFormatters)
        every { DateTimeFormatters.dateMMMdYYYY } returns
            DateTimeFormat.forPattern("MMM d, yyyy").withLocale(Locale.US)
        every { DateTimeFormatters.timeFormatter } returns
            DateTimeFormat.forPattern("HH:mm").withLocale(Locale.US)
    }

    @AfterAll
    fun tearDown() {
        Locale.setDefault(defaultLocale)
        unmockkObject(DateTimeFormatters)
    }

    @Test
    fun `GIVEN full reading set WHEN convert THEN content matches expected`() {
        // Arrange
        val coinIndicators = createCoinIndicators(
            readings = listOf(
                createReading(type = Type.GALAXY_SCORE, timeframe = null, value = BigDecimal("68"), signal = Signal.BULLISH),
                createReading(type = Type.SENTIMENT, timeframe = null, value = BigDecimal("61"), signal = Signal.NEUTRAL),
                createReading(type = Type.RSI, timeframe = Timeframe.DAY, value = BigDecimal("58.4"), signal = Signal.BULLISH),
                createReading(type = Type.MACD, timeframe = Timeframe.DAY, value = BigDecimal("12.34"), signal = Signal.BULLISH),
                createReading(
                    type = Type.MA_CROSS,
                    timeframe = null,
                    value = null,
                    signal = Signal.BEARISH,
                    updatedAt = DateTime(2026, 1, 20, 21, 24, DateTimeZone.UTC),
                ),
            ),
        )

        // Act
        val actual = TokenSentimentConverter(timeframe = Timeframe.DAY).convert(coinIndicators)

        // Assert
        val content = actual as TokenSentimentUM.Content
        assertThat(content.sentiment).isEqualTo(resourceReference(R.string.token_summary_positive_outlook_title))
        assertThat(content.totalScore).isEqualTo(2)
        assertThat(content.lastUpdate).isEqualTo(expectedLastUpdate(DateTime(2026, 1, 20, 21, 24, DateTimeZone.UTC)))
        // TangemBadgeUM is not a data class — compare structural projections of the rows instead
        assertThat(content.indicators.map(::projection))
            .containsExactly(
                RowProjection(IndicatorType.GalaxyScore, positiveBadge(), TangemBadgeColor.Green, stringReference("68")),
                RowProjection(IndicatorType.Sentiment, neutralBadge(), TangemBadgeColor.Blue, stringReference("61")),
                RowProjection(IndicatorType.RSI, positiveBadge(), TangemBadgeColor.Green, stringReference("58.4")),
                RowProjection(IndicatorType.MACD, positiveBadge(), TangemBadgeColor.Green, stringReference("12.34")),
                RowProjection(IndicatorType.MA_CROSS, negativeBadge(), TangemBadgeColor.Red, stringReference("—")),
            )
            .inOrder()
    }

    @Test
    fun `GIVEN readings for all timeframes WHEN convert with week THEN rsi and macd use week readings`() {
        // Arrange
        val coinIndicators = createCoinIndicators(
            readings = listOf(
                createReading(type = Type.RSI, timeframe = Timeframe.DAY, signal = Signal.BULLISH),
                createReading(type = Type.RSI, timeframe = Timeframe.WEEK, signal = Signal.BEARISH),
                createReading(type = Type.RSI, timeframe = Timeframe.MONTH, signal = Signal.NEUTRAL),
                createReading(type = Type.MACD, timeframe = Timeframe.DAY, signal = Signal.BULLISH),
                createReading(type = Type.MACD, timeframe = Timeframe.WEEK, signal = Signal.NEUTRAL),
            ),
        )

        // Act
        val actual = TokenSentimentConverter(timeframe = Timeframe.WEEK).convert(coinIndicators)

        // Assert
        val content = actual as TokenSentimentUM.Content
        assertThat(content.rowSentimentText(IndicatorType.RSI)).isEqualTo(negativeBadge())
        assertThat(content.rowSentimentText(IndicatorType.MACD)).isEqualTo(neutralBadge())
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN signals WHEN convert THEN total score and outlook match`(model: ScoreModel) {
        // Arrange
        val types = listOf(Type.GALAXY_SCORE, Type.SENTIMENT, Type.RSI, Type.MACD, Type.MA_CROSS)
        val coinIndicators = createCoinIndicators(
            readings = types.zip(model.signals) { type, signal ->
                val timeframe = if (type == Type.RSI || type == Type.MACD) Timeframe.DAY else null
                createReading(type = type, timeframe = timeframe, signal = signal)
            },
        )

        // Act
        val actual = TokenSentimentConverter(timeframe = Timeframe.DAY).convert(coinIndicators)

        // Assert
        val content = actual as TokenSentimentUM.Content
        assertThat(content.totalScore).isEqualTo(model.expectedScore)
        assertThat(content.sentiment).isEqualTo(resourceReference(model.expectedOutlook))
    }

    private fun provideTestModels() = listOf(
        ScoreModel(
            signals = List(size = 5) { Signal.BULLISH },
            expectedScore = 5,
            expectedOutlook = R.string.token_summary_positive_outlook_title,
        ),
        ScoreModel(
            signals = List(size = 5) { Signal.BEARISH },
            expectedScore = -5,
            expectedOutlook = R.string.token_summary_negative_outlook_title,
        ),
        ScoreModel(
            signals = listOf(Signal.BULLISH, Signal.BEARISH, Signal.NEUTRAL, Signal.NOT_AVAILABLE, Signal.BULLISH),
            expectedScore = 1,
            expectedOutlook = R.string.token_summary_positive_outlook_title,
        ),
        ScoreModel(
            signals = listOf(Signal.BULLISH, Signal.BEARISH, Signal.NEUTRAL, Signal.NEUTRAL, Signal.NEUTRAL),
            expectedScore = 0,
            expectedOutlook = R.string.token_summary_neutral_outlook_title,
        ),
    )

    @Test
    fun `GIVEN present but non-signal readings WHEN convert THEN rows show the None badge`() {
        // Arrange
        val coinIndicators = createCoinIndicators(
            readings = listOf(
                createReading(type = Type.RSI, timeframe = Timeframe.DAY, signal = Signal.INSUFFICIENT_DATA),
                createReading(type = Type.MACD, timeframe = Timeframe.DAY, signal = Signal.NOT_APPLICABLE),
                createReading(type = Type.MA_CROSS, timeframe = null, signal = Signal.NOT_AVAILABLE),
            ),
        )

        // Act
        val actual = TokenSentimentConverter(timeframe = Timeframe.DAY).convert(coinIndicators)

        // Assert — a present reading always yields a Content row; a non-signal state renders the Gray "None" badge
        val content = actual as TokenSentimentUM.Content
        listOf(IndicatorType.RSI, IndicatorType.MACD, IndicatorType.MA_CROSS).forEach { type ->
            val row = content.row(type)
            assertThat(row).isInstanceOf(TokenIndicatorUM.Content::class.java)
            assertThat((row as TokenIndicatorUM.Content).sentimentBadge.text)
                .isEqualTo(resourceReference(R.string.common_none))
            assertThat(row.sentimentBadge.color).isEqualTo(TangemBadgeColor.Gray)
        }
    }

    @Test
    fun `GIVEN missing readings WHEN convert THEN rows are no data`() {
        // Arrange
        val coinIndicators = createCoinIndicators(
            readings = listOf(createReading(type = Type.RSI, timeframe = Timeframe.DAY, signal = Signal.BULLISH)),
        )

        // Act
        val actual = TokenSentimentConverter(timeframe = Timeframe.DAY).convert(coinIndicators)

        // Assert
        val content = actual as TokenSentimentUM.Content
        assertThat(content.row(IndicatorType.RSI)).isInstanceOf(TokenIndicatorUM.Content::class.java)
        assertThat(content.row(IndicatorType.GalaxyScore)).isEqualTo(TokenIndicatorUM.NoData(IndicatorType.GalaxyScore))
        assertThat(content.row(IndicatorType.Sentiment)).isEqualTo(TokenIndicatorUM.NoData(IndicatorType.Sentiment))
        assertThat(content.row(IndicatorType.MACD)).isEqualTo(TokenIndicatorUM.NoData(IndicatorType.MACD))
        assertThat(content.row(IndicatorType.MA_CROSS)).isEqualTo(TokenIndicatorUM.NoData(IndicatorType.MA_CROSS))
    }

    @Test
    fun `GIVEN several updated at values WHEN convert THEN last update is the latest`() {
        // Arrange
        val coinIndicators = createCoinIndicators(
            readings = listOf(
                createReading(
                    type = Type.RSI,
                    timeframe = Timeframe.DAY,
                    updatedAt = DateTime(2026, 1, 19, 9, 0, DateTimeZone.UTC),
                ),
                createReading(
                    type = Type.GALAXY_SCORE,
                    timeframe = null,
                    updatedAt = DateTime(2026, 1, 20, 21, 24, DateTimeZone.UTC),
                ),
            ),
        )

        // Act
        val actual = TokenSentimentConverter(timeframe = Timeframe.DAY).convert(coinIndicators)

        // Assert
        assertThat((actual as TokenSentimentUM.Content).lastUpdate)
            .isEqualTo(expectedLastUpdate(DateTime(2026, 1, 20, 21, 24, DateTimeZone.UTC)))
    }

    @Test
    fun `GIVEN no updated at values WHEN convert THEN last update is empty`() {
        // Arrange
        val coinIndicators = createCoinIndicators(
            readings = listOf(createReading(type = Type.RSI, timeframe = Timeframe.DAY, updatedAt = null)),
        )

        // Act
        val actual = TokenSentimentConverter(timeframe = Timeframe.DAY).convert(coinIndicators)

        // Assert
        assertThat((actual as TokenSentimentUM.Content).lastUpdate).isEqualTo(TextReference.EMPTY)
    }

    @Test
    fun `GIVEN any readings WHEN convert THEN rows follow indicator type order`() {
        // Arrange
        val coinIndicators = createCoinIndicators(readings = emptyList())

        // Act
        val actual = TokenSentimentConverter(timeframe = Timeframe.DAY).convert(coinIndicators)

        // Assert
        val content = actual as TokenSentimentUM.Content
        assertThat(content.indicators.map(TokenIndicatorUM::indicatorType))
            .containsExactlyElementsIn(IndicatorType.entries)
            .inOrder()
    }

    private fun TokenSentimentUM.Content.row(indicatorType: IndicatorType): TokenIndicatorUM {
        return indicators.first { it.indicatorType == indicatorType }
    }

    private fun TokenSentimentUM.Content.rowSentimentText(indicatorType: IndicatorType): TextReference {
        return (row(indicatorType) as TokenIndicatorUM.Content).sentimentBadge.text
    }

    private fun projection(row: TokenIndicatorUM): RowProjection {
        return when (row) {
            is TokenIndicatorUM.Content -> RowProjection(
                indicatorType = row.indicatorType,
                sentiment = row.sentimentBadge.text,
                color = row.sentimentBadge.color,
                score = row.scoreBadge?.text,
            )
            is TokenIndicatorUM.NoData,
            is TokenIndicatorUM.Loading,
            -> RowProjection(indicatorType = row.indicatorType, sentiment = null, color = null, score = null)
        }
    }

    private data class RowProjection(
        val indicatorType: IndicatorType,
        val sentiment: TextReference?,
        val color: TangemBadgeColor?,
        val score: TextReference?,
    )

    private fun createCoinIndicators(symbol: String = "BTC", readings: List<CoinIndicators.Reading>): CoinIndicators {
        return CoinIndicators(symbol = symbol, readings = readings)
    }

    @Suppress("LongParameterList")
    private fun createReading(
        type: Type = Type.RSI,
        timeframe: Timeframe? = Timeframe.DAY,
        value: BigDecimal? = BigDecimal("72.21"),
        signal: Signal = Signal.BULLISH,
        subLabel: String? = null,
        updatedAt: DateTime? = null,
    ): CoinIndicators.Reading {
        return CoinIndicators.Reading(
            type = type,
            timeframe = timeframe,
            value = value,
            signal = signal,
            subLabel = subLabel,
            updatedAt = updatedAt,
        )
    }

    internal data class ScoreModel(
        val signals: List<Signal>,
        val expectedScore: Int,
        @StringRes val expectedOutlook: Int,
    )

    // Computed via the same production util so the expected date matches regardless of formatter/timezone.
    private fun expectedLastUpdate(dateTime: DateTime): TextReference = resourceReference(
        R.string.token_summary_last_update_subtitle,
        wrappedList(dateTime.millis.toDateFormatWithTodayYesterday()),
    )

    private fun positiveBadge(): TextReference = resourceReference(R.string.common_positive)

    private fun negativeBadge(): TextReference = resourceReference(R.string.common_negative)

    private fun neutralBadge(): TextReference = resourceReference(R.string.common_neutral)
}