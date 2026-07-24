package com.tangem.features.foryou.impl.tokensummary.model.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.markets.CoinIndicators
import com.tangem.domain.markets.CoinIndicators.Reading.Signal
import com.tangem.domain.markets.CoinIndicators.Reading.Type
import com.tangem.features.foryou.impl.tokensummary.entity.PeriodPickerUM
import com.tangem.features.foryou.impl.tokensummary.entity.TokenSentimentUM
import com.tangem.features.foryou.impl.tokensummary.entity.TokenSummaryUm
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class SetTokenSentimentTransformerTest {

    @Test
    fun `GIVEN indicators with all null values WHEN transform THEN period picker and sentiment are Empty`() {
        // Arrange — the indicators exist but carry no readings' values at all
        val indicators = coinIndicators(
            reading(type = Type.RSI, value = null),
            reading(type = Type.GALAXY_SCORE, value = null),
        )

        // Act
        val result = transform(indicators, prevPeriodPicker = PeriodPickerUM.Loading)

        // Assert — value-less indicators disable the picker and collapse the sentiment section
        assertThat(result.periodPicker).isEqualTo(PeriodPickerUM.Empty)
        assertThat(result.tokenSentiment).isEqualTo(TokenSentimentUM.Empty)
    }

    @Test
    fun `GIVEN indicators with a valued reading WHEN transform THEN period picker kept and sentiment is Content`() {
        // Arrange — a single valued (timeframe-agnostic) reading is enough to keep the section populated
        val indicators = coinIndicators(
            reading(type = Type.GALAXY_SCORE, value = BigDecimal("50"), signal = Signal.BULLISH),
        )

        // Act
        val result = transform(indicators, prevPeriodPicker = PeriodPickerUM.Loading)

        // Assert — the picker is left untouched, sentiment resolves to Content
        assertThat(result.periodPicker).isEqualTo(PeriodPickerUM.Loading)
        assertThat(result.tokenSentiment).isInstanceOf(TokenSentimentUM.Content::class.java)
    }

    @Test
    fun `GIVEN null indicators WHEN transform THEN period picker kept and sentiment is Empty`() {
        // Act — no indicators loaded yet
        val result = transform(coinIndicators = null, prevPeriodPicker = PeriodPickerUM.Loading)

        // Assert — absent indicators must not touch the picker, but the sentiment section is Empty
        assertThat(result.periodPicker).isEqualTo(PeriodPickerUM.Loading)
        assertThat(result.tokenSentiment).isEqualTo(TokenSentimentUM.Empty)
    }

    @Test
    fun `GIVEN indicators with no readings WHEN transform THEN period picker and sentiment are Empty`() {
        // Arrange — an empty readings list vacuously satisfies "all values null"
        val indicators = coinIndicators()

        // Act
        val result = transform(indicators, prevPeriodPicker = PeriodPickerUM.Loading)

        // Assert
        assertThat(result.periodPicker).isEqualTo(PeriodPickerUM.Empty)
        assertThat(result.tokenSentiment).isEqualTo(TokenSentimentUM.Empty)
    }

    private fun transform(coinIndicators: CoinIndicators?, prevPeriodPicker: PeriodPickerUM): TokenSummaryUm {
        return SetTokenSentimentTransformer(coinIndicators = coinIndicators, periodId = null)
            .transform(createState(periodPicker = prevPeriodPicker))
    }

    private fun createState(periodPicker: PeriodPickerUM): TokenSummaryUm = TokenSummaryUm(
        header = mockk(),
        periodPicker = periodPicker,
        aiInsight = mockk(),
        tokenSentiment = TokenSentimentUM.Loading,
        onSwapClick = {},
        onPeriodClick = {},
        onInfoClick = {},
        onCloseClick = {},
    )

    private fun coinIndicators(vararg readings: CoinIndicators.Reading): CoinIndicators =
        CoinIndicators(symbol = "BTC", readings = readings.toList())

    private fun reading(
        type: Type,
        value: BigDecimal?,
        signal: Signal = Signal.BULLISH,
    ): CoinIndicators.Reading = CoinIndicators.Reading(
        type = type,
        timeframe = null,
        value = value,
        signal = signal,
        subLabel = null,
        updatedAt = null,
    )
}