package com.tangem.domain.markets

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.markets.CoinIndicators.Reading
import com.tangem.domain.markets.CoinIndicators.Reading.Signal
import com.tangem.domain.markets.CoinIndicators.Reading.Timeframe
import com.tangem.domain.markets.CoinIndicators.Reading.Type
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CoinIndicatorsExtTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FindReading {

        @Test
        fun `GIVEN RSI reading for DAY WHEN findReading DAY THEN that reading is returned`() {
            // Arrange
            val day = reading(Type.RSI, Timeframe.DAY, Signal.POSITIVE)
            val week = reading(Type.RSI, Timeframe.WEEK, Signal.NEGATIVE)
            val indicators = coinIndicators(readings = listOf(day, week))

            // Act
            val actual = indicators.findReading(Type.RSI, Timeframe.DAY)

            // Assert
            assertThat(actual).isEqualTo(day)
        }

        @Test
        fun `GIVEN RSI only for WEEK WHEN findReading DAY THEN null`() {
            // Arrange
            val indicators = coinIndicators(readings = listOf(reading(Type.RSI, Timeframe.WEEK, Signal.POSITIVE)))

            // Act
            val actual = indicators.findReading(Type.RSI, Timeframe.DAY)

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN SENTIMENT only for DAY WHEN findReading other timeframes THEN null`() {
            // Arrange — every indicator carries a timeframe now, so the match is exact for all types
            val sentiment = reading(Type.SENTIMENT, Timeframe.DAY, Signal.POSITIVE)
            val indicators = coinIndicators(readings = listOf(sentiment))

            // Act & Assert
            assertThat(indicators.findReading(Type.SENTIMENT, Timeframe.DAY)).isEqualTo(sentiment)
            assertThat(indicators.findReading(Type.SENTIMENT, Timeframe.WEEK)).isNull()
            assertThat(indicators.findReading(Type.SENTIMENT, Timeframe.MONTH)).isNull()
        }

        @Test
        fun `GIVEN no reading of the requested type WHEN findReading THEN null`() {
            // Arrange
            val indicators = coinIndicators(readings = listOf(reading(Type.RSI, Timeframe.DAY, Signal.POSITIVE)))

            // Act
            val actual = indicators.findReading(Type.MACD, Timeframe.DAY)

            // Assert
            assertThat(actual).isNull()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TotalSentimentScore {

        @ParameterizedTest
        @ProvideTestModels
        fun totalSentimentScore(model: ScoreModel) {
            // Arrange
            val indicators = coinIndicators(readings = model.readings)

            // Act
            val actual = indicators.totalSentimentScore(model.timeframe)

            // Assert
            assertThat(actual).isEqualTo(model.expectedScore)
        }

        private fun provideTestModels() = listOf(
            ScoreModel(
                description = "all five positive for DAY sum to +5",
                readings = Type.entries.map { reading(it, Timeframe.DAY, Signal.POSITIVE) },
                timeframe = Timeframe.DAY,
                expectedScore = 5,
            ),
            ScoreModel(
                description = "all five negative for DAY sum to -5",
                readings = Type.entries.map { reading(it, Timeframe.DAY, Signal.NEGATIVE) },
                timeframe = Timeframe.DAY,
                expectedScore = -5,
            ),
            ScoreModel(
                description = "mixed signals net out",
                readings = listOf(
                    reading(Type.RSI, Timeframe.DAY, Signal.POSITIVE),
                    reading(Type.MACD, Timeframe.DAY, Signal.POSITIVE),
                    reading(Type.MA_CROSS, Timeframe.DAY, Signal.NEGATIVE),
                    reading(Type.GALAXY_SCORE, Timeframe.DAY, Signal.NEUTRAL),
                    reading(Type.SENTIMENT, Timeframe.DAY, Signal.POSITIVE),
                ),
                timeframe = Timeframe.DAY,
                expectedScore = 2,
            ),
            ScoreModel(
                description = "non-actionable signals contribute zero",
                readings = listOf(
                    reading(Type.RSI, Timeframe.DAY, Signal.INSUFFICIENT_DATA),
                    reading(Type.MACD, Timeframe.DAY, Signal.NOT_AVAILABLE),
                    reading(Type.MA_CROSS, Timeframe.DAY, Signal.NOT_AVAILABLE),
                    reading(Type.GALAXY_SCORE, Timeframe.DAY, Signal.NEUTRAL),
                    reading(Type.SENTIMENT, Timeframe.DAY, Signal.POSITIVE),
                ),
                timeframe = Timeframe.DAY,
                expectedScore = 1,
            ),
            ScoreModel(
                description = "no readings for the coin scores zero",
                readings = emptyList(),
                timeframe = Timeframe.DAY,
                expectedScore = 0,
            ),
            ScoreModel(
                description = "timeframe selects the matching RSI/MACD reading",
                readings = listOf(
                    reading(Type.RSI, Timeframe.DAY, Signal.POSITIVE),
                    reading(Type.RSI, Timeframe.WEEK, Signal.NEGATIVE),
                    reading(Type.MACD, Timeframe.DAY, Signal.POSITIVE),
                    reading(Type.MACD, Timeframe.WEEK, Signal.NEGATIVE),
                ),
                timeframe = Timeframe.WEEK,
                expectedScore = -2,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SentimentScaleMax {

        @ParameterizedTest
        @ProvideTestModels
        fun sentimentScaleMax(model: ScaleModel) {
            // Arrange
            val indicators = coinIndicators(readings = model.readings)

            // Act
            val actual = indicators.sentimentScaleMax(model.timeframe)

            // Assert
            assertThat(actual).isEqualTo(model.expectedMax)
        }

        private fun provideTestModels() = listOf(
            ScaleModel(
                description = "all five loaded for DAY -> max 5",
                readings = Type.entries.map { reading(it, Timeframe.DAY, Signal.POSITIVE) },
                timeframe = Timeframe.DAY,
                expectedMax = 5,
            ),
            ScaleModel(
                description = "one NOT_AVAILABLE indicator drops max to 4",
                readings = listOf(
                    reading(Type.RSI, Timeframe.DAY, Signal.POSITIVE),
                    reading(Type.MACD, Timeframe.DAY, Signal.NEGATIVE),
                    reading(Type.MA_CROSS, Timeframe.DAY, Signal.POSITIVE),
                    reading(Type.GALAXY_SCORE, Timeframe.DAY, Signal.NEUTRAL),
                    reading(Type.SENTIMENT, Timeframe.DAY, Signal.NOT_AVAILABLE),
                ),
                timeframe = Timeframe.DAY,
                expectedMax = 4,
            ),
            ScaleModel(
                description = "two missing readings drop max to 3",
                readings = listOf(
                    reading(Type.RSI, Timeframe.DAY, Signal.POSITIVE),
                    reading(Type.MACD, Timeframe.DAY, Signal.NEGATIVE),
                    reading(Type.MA_CROSS, Timeframe.DAY, Signal.NEUTRAL),
                    // GALAXY_SCORE and SENTIMENT absent
                ),
                timeframe = Timeframe.DAY,
                expectedMax = 3,
            ),
            ScaleModel(
                description = "NEUTRAL counts as loaded (keeps its position)",
                readings = Type.entries.map { reading(it, Timeframe.DAY, Signal.NEUTRAL) },
                timeframe = Timeframe.DAY,
                expectedMax = 5,
            ),
            ScaleModel(
                description = "all non-actionable clamps to 1",
                readings = listOf(
                    reading(Type.RSI, Timeframe.DAY, Signal.INSUFFICIENT_DATA),
                    reading(Type.MACD, Timeframe.DAY, Signal.NOT_AVAILABLE),
                    reading(Type.MA_CROSS, Timeframe.DAY, Signal.NOT_AVAILABLE),
                    reading(Type.GALAXY_SCORE, Timeframe.DAY, Signal.INSUFFICIENT_DATA),
                    reading(Type.SENTIMENT, Timeframe.DAY, Signal.NOT_AVAILABLE),
                ),
                timeframe = Timeframe.DAY,
                expectedMax = 1,
            ),
            ScaleModel(
                description = "mix of missing + non-actionable counts only the loaded ones",
                readings = listOf(
                    reading(Type.RSI, Timeframe.DAY, Signal.POSITIVE),
                    reading(Type.MACD, Timeframe.DAY, Signal.INSUFFICIENT_DATA),
                    reading(Type.MA_CROSS, Timeframe.DAY, Signal.NOT_AVAILABLE),
                    // GALAXY_SCORE absent
                    reading(Type.SENTIMENT, Timeframe.DAY, Signal.NEGATIVE),
                ),
                timeframe = Timeframe.DAY,
                expectedMax = 2,
            ),
            ScaleModel(
                description = "no readings clamps to 1",
                readings = emptyList(),
                timeframe = Timeframe.DAY,
                expectedMax = 1,
            ),
        )
    }

    internal data class ScoreModel(
        val description: String,
        val readings: List<Reading>,
        val timeframe: Timeframe,
        val expectedScore: Int,
    ) {
        override fun toString(): String = description
    }

    internal data class ScaleModel(
        val description: String,
        val readings: List<Reading>,
        val timeframe: Timeframe,
        val expectedMax: Int,
    ) {
        override fun toString(): String = description
    }

    private fun coinIndicators(symbol: String = "BTC", readings: List<Reading>) = CoinIndicators(
        symbol = symbol,
        readings = readings,
    )

    private fun reading(type: Type, timeframe: Timeframe, signal: Signal) = Reading(
        type = type,
        name = type.name,
        timeframe = timeframe,
        value = null,
        signal = signal,
        updatedAt = null,
    )
}