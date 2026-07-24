package com.tangem.features.foryou.impl.tokensummary.model.converter

import androidx.annotation.StringRes
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.core.ui.utils.formatAsDateTime
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
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TokenSentimentConverterTest {

    private lateinit var defaultLocale: Locale

    @BeforeAll
    fun setUp() {
        defaultLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)

        // The converter renders "last update" via formatAsDateTime(dateDDMMYYYY); pin the formatter so the
        // expected value (computed through the same util) is deterministic regardless of the host locale.
        mockkObject(DateTimeFormatters)
        every { DateTimeFormatters.dateDDMMYYYY } returns
            DateTimeFormat.forPattern("dd.MM.yyyy").withLocale(Locale.US)
    }

    @AfterAll
    fun tearDown() {
        Locale.setDefault(defaultLocale)
        unmockkObject(DateTimeFormatters)
    }

    @Test
    fun `GIVEN full reading set WHEN convert THEN content matches expected`() {
        // Arrange — MA_CROSS is present but value-less, so its row must fall back to NoData
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
        // All five indicators loaded (BULLISH/BEARISH/NEUTRAL), so the scale spans the full -5..5.
        assertThat(content.scaleMax).isEqualTo(5)
        assertThat(content.lastUpdate).isEqualTo(expectedLastUpdate(DateTime(2026, 1, 20, 21, 24, DateTimeZone.UTC)))
        assertThat(content.indicators.map(::projection))
            .containsExactly(
                RowProjection(IndicatorType.GalaxyScore, positive(), TangemBadge.Status.Success, stringReference("68")),
                RowProjection(IndicatorType.Sentiment, neutral(), TangemBadge.Status.Info, stringReference("61")),
                RowProjection(IndicatorType.RSI, positive(), TangemBadge.Status.Success, stringReference("58.4")),
                RowProjection(IndicatorType.MACD, positive(), TangemBadge.Status.Success, stringReference("12.34")),
                // Present but value-less -> NoData (no badge, no score)
                RowProjection(IndicatorType.MA_CROSS, sentiment = null, status = null, score = null),
            )
            .inOrder()
    }

    @Test
    fun `GIVEN one indicator that can't load WHEN convert THEN scaleMax drops by one`() {
        // Arrange — SENTIMENT is unavailable (can't load), so the scale shrinks from -5..5 to -4..4.
        val coinIndicators = createCoinIndicators(
            readings = listOf(
                createReading(type = Type.GALAXY_SCORE, timeframe = null, value = BigDecimal("68"), signal = Signal.BULLISH),
                createReading(type = Type.SENTIMENT, timeframe = null, value = null, signal = Signal.NOT_AVAILABLE),
                createReading(type = Type.RSI, timeframe = Timeframe.DAY, value = BigDecimal("58.4"), signal = Signal.BULLISH),
                createReading(type = Type.MACD, timeframe = Timeframe.DAY, value = BigDecimal("12.34"), signal = Signal.BEARISH),
                createReading(type = Type.MA_CROSS, timeframe = null, value = null, signal = Signal.NEUTRAL),
            ),
        )

        // Act
        val content = TokenSentimentConverter(timeframe = Timeframe.DAY)
            .convert(coinIndicators) as TokenSentimentUM.Content

        // Assert — 4 loaded indicators (SENTIMENT excluded); score unchanged (2 bullish - 1 bearish).
        assertThat(content.scaleMax).isEqualTo(4)
        assertThat(content.totalScore).isEqualTo(1)
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
        assertThat(content.rowSentimentText(IndicatorType.RSI)).isEqualTo(negative())
        assertThat(content.rowSentimentStatus(IndicatorType.RSI)).isEqualTo(TangemBadge.Status.Error)
        assertThat(content.rowSentimentText(IndicatorType.MACD)).isEqualTo(neutral())
        assertThat(content.rowSentimentStatus(IndicatorType.MACD)).isEqualTo(TangemBadge.Status.Info)
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

    @ParameterizedTest
    @MethodSource("provideSignalModels")
    fun `GIVEN a reading signal WHEN convert THEN row badge text and status match`(model: SignalModel) {
        // Arrange — a single valued reading, so the row is always Content and the signal drives the badge
        val coinIndicators = createCoinIndicators(
            readings = listOf(createReading(type = Type.RSI, timeframe = Timeframe.DAY, signal = model.signal)),
        )

        // Act
        val actual = TokenSentimentConverter(timeframe = Timeframe.DAY).convert(coinIndicators)

        // Assert
        val row = (actual as TokenSentimentUM.Content).row(IndicatorType.RSI)
        assertThat(row).isInstanceOf(TokenIndicatorUM.Content::class.java)
        assertThat((row as TokenIndicatorUM.Content).sentimentBadgeText).isEqualTo(resourceReference(model.expectedText))
        assertThat(row.sentimentBadgeStatus).isEqualTo(model.expectedStatus)
    }

    private fun provideSignalModels() = listOf(
        SignalModel(Signal.BULLISH, R.string.common_positive, TangemBadge.Status.Success),
        SignalModel(Signal.BEARISH, R.string.common_negative, TangemBadge.Status.Error),
        SignalModel(Signal.NEUTRAL, R.string.common_neutral, TangemBadge.Status.Info),
        SignalModel(Signal.INSUFFICIENT_DATA, R.string.common_none, TangemBadge.Status.Neutral),
        SignalModel(Signal.NOT_APPLICABLE, R.string.common_none, TangemBadge.Status.Neutral),
        SignalModel(Signal.NOT_AVAILABLE, R.string.common_none, TangemBadge.Status.Neutral),
    )

    @Test
    fun `GIVEN score text WHEN convert THEN it is the value formatted with two decimals`() {
        // Arrange
        val coinIndicators = createCoinIndicators(
            readings = listOf(createReading(type = Type.RSI, timeframe = Timeframe.DAY, value = BigDecimal("58.4"))),
        )

        // Act
        val actual = TokenSentimentConverter(timeframe = Timeframe.DAY).convert(coinIndicators)

        // Assert
        val row = (actual as TokenSentimentUM.Content).row(IndicatorType.RSI) as TokenIndicatorUM.Content
        assertThat(row.scoreBadgeText).isEqualTo(stringReference("58.4"))
    }

    @Test
    fun `GIVEN no readings WHEN convert THEN result is Empty with rows in indicator type order`() {
        // Arrange
        val coinIndicators = createCoinIndicators(readings = emptyList())

        // Act
        val actual = TokenSentimentConverter(timeframe = Timeframe.DAY).convert(coinIndicators)

        // Assert — no data at all collapses to the Empty state, whose rows still follow the indicator order
        assertThat(actual).isInstanceOf(TokenSentimentUM.Empty::class.java)
        assertThat(actual.indicators.map(TokenIndicatorUM::indicatorType))
            .containsExactlyElementsIn(IndicatorType.entries)
            .inOrder()
    }

    @Test
    fun `GIVEN readings present but all values null WHEN convert THEN result is Empty`() {
        // Arrange — readings exist for the timeframe but none carries a value
        val coinIndicators = createCoinIndicators(
            readings = listOf(
                createReading(type = Type.RSI, timeframe = Timeframe.DAY, value = null, signal = Signal.BULLISH),
                createReading(type = Type.GALAXY_SCORE, timeframe = null, value = null, signal = Signal.BEARISH),
            ),
        )

        // Act
        val actual = TokenSentimentConverter(timeframe = Timeframe.DAY).convert(coinIndicators)

        // Assert
        assertThat(actual).isInstanceOf(TokenSentimentUM.Empty::class.java)
    }

    @Test
    fun `GIVEN a reading present with null value WHEN convert THEN that row is NoData`() {
        // Arrange — RSI carries a value (keeps the whole thing Content); GalaxyScore is present but value-less
        val coinIndicators = createCoinIndicators(
            readings = listOf(
                createReading(type = Type.RSI, timeframe = Timeframe.DAY, value = BigDecimal("50"), signal = Signal.BULLISH),
                createReading(type = Type.GALAXY_SCORE, timeframe = null, value = null, signal = Signal.BULLISH),
            ),
        )

        // Act
        val actual = TokenSentimentConverter(timeframe = Timeframe.DAY).convert(coinIndicators)

        // Assert — a present-but-value-less reading collapses to NoData, not a Content row
        val content = actual as TokenSentimentUM.Content
        assertThat(content.row(IndicatorType.RSI)).isInstanceOf(TokenIndicatorUM.Content::class.java)
        assertThat(content.row(IndicatorType.GalaxyScore)).isEqualTo(TokenIndicatorUM.NoData(IndicatorType.GalaxyScore))
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

    private fun TokenSentimentUM.Content.row(indicatorType: IndicatorType): TokenIndicatorUM {
        return indicators.first { it.indicatorType == indicatorType }
    }

    private fun TokenSentimentUM.Content.rowSentimentText(indicatorType: IndicatorType): TextReference {
        return (row(indicatorType) as TokenIndicatorUM.Content).sentimentBadgeText
    }

    private fun TokenSentimentUM.Content.rowSentimentStatus(indicatorType: IndicatorType): TangemBadge.Status {
        return (row(indicatorType) as TokenIndicatorUM.Content).sentimentBadgeStatus
    }

    private fun projection(row: TokenIndicatorUM): RowProjection {
        return when (row) {
            is TokenIndicatorUM.Content -> RowProjection(
                indicatorType = row.indicatorType,
                sentiment = row.sentimentBadgeText,
                status = row.sentimentBadgeStatus,
                score = row.scoreBadgeText,
            )
            is TokenIndicatorUM.NoData,
            is TokenIndicatorUM.Loading,
            -> RowProjection(indicatorType = row.indicatorType, sentiment = null, status = null, score = null)
        }
    }

    private data class RowProjection(
        val indicatorType: IndicatorType,
        val sentiment: TextReference?,
        val status: TangemBadge.Status?,
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

    internal data class SignalModel(
        val signal: Signal,
        @StringRes val expectedText: Int,
        val expectedStatus: TangemBadge.Status,
    )

    // Computed via the same production util so the expected date matches regardless of formatter/timezone.
    private fun expectedLastUpdate(dateTime: DateTime): TextReference = resourceReference(
        R.string.token_summary_last_update_subtitle,
        wrappedList(dateTime.millis.formatAsDateTime(DateTimeFormatters.dateDDMMYYYY)),
    )

    private fun positive(): TextReference = resourceReference(R.string.common_positive)

    private fun negative(): TextReference = resourceReference(R.string.common_negative)

    private fun neutral(): TextReference = resourceReference(R.string.common_neutral)
}