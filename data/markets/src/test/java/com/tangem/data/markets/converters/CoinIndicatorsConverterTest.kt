package com.tangem.data.markets.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.markets.models.response.GetCoinIndicatorsResponse.Asset
import com.tangem.datasource.api.markets.models.response.GetCoinIndicatorsResponse.Asset.Indicator
import com.tangem.domain.markets.CoinIndicators
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CoinIndicatorsConverterTest {

    @Test
    fun `GIVEN asset with readings WHEN convert THEN symbol and readings are mapped`() {
        // Arrange
        val asset = asset(
            symbol = "BTC",
            indicators = listOf(indicator(type = Indicator.Type.RSI, timeframe = Indicator.Timeframe.H24)),
        )

        // Act
        val actual = CoinIndicatorsConverter.convert(asset)

        // Assert
        assertThat(actual.symbol).isEqualTo("BTC")
        assertThat(actual.readings).hasSize(1)
        assertThat(actual.readings.single().type).isEqualTo(CoinIndicators.Reading.Type.RSI)
        assertThat(actual.readings.single().timeframe).isEqualTo(CoinIndicators.Reading.Timeframe.DAY)
    }

    @Test
    fun `GIVEN full reading fields WHEN convert THEN value subLabel and updatedAt are carried over`() {
        // Arrange
        val asset = asset(
            indicators = listOf(
                indicator(
                    type = Indicator.Type.RSI,
                    timeframe = Indicator.Timeframe.H24,
                    value = BigDecimal("42.5"),
                    label = Indicator.Signal.BULLISH,
                    subLabel = "Oversold",
                ),
            ),
        )

        // Act
        val reading = CoinIndicatorsConverter.convert(asset).readings.single()

        // Assert
        assertThat(reading.value).isEqualTo(BigDecimal("42.5"))
        assertThat(reading.subLabel).isEqualTo("Oversold")
        assertThat(reading.signal).isEqualTo(CoinIndicators.Reading.Signal.BULLISH)
    }

    @Test
    fun `GIVEN reading of UNKNOWN type WHEN convert THEN it is dropped`() {
        // Arrange — a type this app version can't display must not surface
        val asset = asset(
            indicators = listOf(
                indicator(type = Indicator.Type.UNKNOWN, timeframe = Indicator.Timeframe.H24),
                indicator(type = Indicator.Type.RSI, timeframe = Indicator.Timeframe.H24),
            ),
        )

        // Act
        val actual = CoinIndicatorsConverter.convert(asset)

        // Assert
        assertThat(actual.readings.map { it.type }).containsExactly(CoinIndicators.Reading.Type.RSI)
    }

    @Test
    fun `GIVEN reading with UNKNOWN timeframe WHEN convert THEN it is dropped`() {
        // Arrange
        val asset = asset(
            indicators = listOf(
                indicator(type = Indicator.Type.RSI, timeframe = Indicator.Timeframe.UNKNOWN),
                indicator(type = Indicator.Type.MACD, timeframe = Indicator.Timeframe.D7),
            ),
        )

        // Act
        val actual = CoinIndicatorsConverter.convert(asset)

        // Assert
        assertThat(actual.readings.map { it.type }).containsExactly(CoinIndicators.Reading.Type.MACD)
    }

    @Test
    fun `GIVEN reading with null timeframe WHEN convert THEN it is kept as timeframe-agnostic`() {
        // Arrange — MA_CROSS / GALAXY_SCORE / SENTIMENT legitimately have no timeframe
        val asset = asset(
            indicators = listOf(indicator(type = Indicator.Type.SENTIMENT, timeframe = null)),
        )

        // Act
        val reading = CoinIndicatorsConverter.convert(asset).readings.single()

        // Assert
        assertThat(reading.type).isEqualTo(CoinIndicators.Reading.Type.SENTIMENT)
        assertThat(reading.timeframe).isNull()
    }

    @ParameterizedTest
    @MethodSource("provideTypeModels")
    fun `map indicator type`(model: TypeModel) {
        // Act
        val actual = CoinIndicatorsConverter.convert(
            asset(indicators = listOf(indicator(type = model.dto, timeframe = null))),
        )

        // Assert
        assertThat(actual.readings.single().type).isEqualTo(model.expected)
    }

    private fun provideTypeModels() = listOf(
        TypeModel(Indicator.Type.RSI, CoinIndicators.Reading.Type.RSI),
        TypeModel(Indicator.Type.MACD, CoinIndicators.Reading.Type.MACD),
        TypeModel(Indicator.Type.MA_CROSS, CoinIndicators.Reading.Type.MA_CROSS),
        TypeModel(Indicator.Type.GALAXY_SCORE, CoinIndicators.Reading.Type.GALAXY_SCORE),
        TypeModel(Indicator.Type.SENTIMENT, CoinIndicators.Reading.Type.SENTIMENT),
    )

    @ParameterizedTest
    @MethodSource("provideTimeframeModels")
    fun `map indicator timeframe`(model: TimeframeModel) {
        // Act
        val actual = CoinIndicatorsConverter.convert(
            asset(indicators = listOf(indicator(type = Indicator.Type.RSI, timeframe = model.dto))),
        )

        // Assert
        assertThat(actual.readings.single().timeframe).isEqualTo(model.expected)
    }

    private fun provideTimeframeModels() = listOf(
        TimeframeModel(Indicator.Timeframe.H24, CoinIndicators.Reading.Timeframe.DAY),
        TimeframeModel(Indicator.Timeframe.D7, CoinIndicators.Reading.Timeframe.WEEK),
        TimeframeModel(Indicator.Timeframe.M1, CoinIndicators.Reading.Timeframe.MONTH),
    )

    @ParameterizedTest
    @MethodSource("provideSignalModels")
    fun `map indicator signal`(model: SignalModel) {
        // Act
        val actual = CoinIndicatorsConverter.convert(
            asset(indicators = listOf(indicator(type = Indicator.Type.RSI, timeframe = null, label = model.dto))),
        )

        // Assert
        assertThat(actual.readings.single().signal).isEqualTo(model.expected)
    }

    private fun provideSignalModels() = listOf(
        SignalModel(Indicator.Signal.BULLISH, CoinIndicators.Reading.Signal.BULLISH),
        SignalModel(Indicator.Signal.BEARISH, CoinIndicators.Reading.Signal.BEARISH),
        SignalModel(Indicator.Signal.NEUTRAL, CoinIndicators.Reading.Signal.NEUTRAL),
        SignalModel(Indicator.Signal.INSUFFICIENT_DATA, CoinIndicators.Reading.Signal.INSUFFICIENT_DATA),
        SignalModel(Indicator.Signal.NOT_APPLICABLE, CoinIndicators.Reading.Signal.NOT_APPLICABLE),
        // Both the explicit `na` and any unknown signal collapse to NOT_AVAILABLE
        SignalModel(Indicator.Signal.NOT_AVAILABLE, CoinIndicators.Reading.Signal.NOT_AVAILABLE),
        SignalModel(Indicator.Signal.UNKNOWN, CoinIndicators.Reading.Signal.NOT_AVAILABLE),
    )

    internal data class TypeModel(val dto: Indicator.Type, val expected: CoinIndicators.Reading.Type)
    internal data class TimeframeModel(val dto: Indicator.Timeframe, val expected: CoinIndicators.Reading.Timeframe)
    internal data class SignalModel(val dto: Indicator.Signal, val expected: CoinIndicators.Reading.Signal)

    private fun asset(symbol: String = "BTC", indicators: List<Indicator>) = Asset(
        symbol = symbol,
        indicators = indicators,
    )

    private fun indicator(
        type: Indicator.Type,
        timeframe: Indicator.Timeframe?,
        value: BigDecimal? = null,
        label: Indicator.Signal = Indicator.Signal.NEUTRAL,
        subLabel: String? = null,
    ) = Indicator(
        type = type,
        timeframe = timeframe,
        value = value,
        label = label,
        subLabel = subLabel,
        updatedAt = null,
    )
}