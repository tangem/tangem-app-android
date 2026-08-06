package com.tangem.features.foryou.impl.tokensummary.model.transformer

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.markets.CoinIndicators
import com.tangem.domain.markets.CoinIndicators.Reading.Signal
import com.tangem.domain.markets.CoinIndicators.Reading.Timeframe
import com.tangem.domain.markets.CoinIndicators.Reading.Type
import com.tangem.features.foryou.impl.tokensummary.entity.BottomButtonUM
import com.tangem.features.foryou.impl.tokensummary.entity.PeriodPickerUM
import com.tangem.features.foryou.impl.tokensummary.entity.TokenSentimentUM
import com.tangem.features.foryou.impl.tokensummary.entity.TokenSummaryUm
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class SetTokenSentimentTransformerTest {

    @Test
    fun `GIVEN indicators with all null values WHEN transform THEN picker is Empty and sentiment is NoOutlook`() {
        // Arrange — the indicators exist but carry no readings' values at all
        val indicators = coinIndicators(
            reading(type = Type.RSI, value = null),
            reading(type = Type.GALAXY_SCORE, value = null),
        )

        // Act
        val result = transform(indicators, prevPeriodPicker = PeriodPickerUM.Loading)

        // Assert — value-less indicators disable the picker; the response did arrive, so there is just no outlook
        assertThat(result.periodPicker).isEqualTo(PeriodPickerUM.Empty)
        assertThat(result.tokenSentiment).isInstanceOf(TokenSentimentUM.Empty.NoOutlook::class.java)
    }

    @Test
    fun `GIVEN indicators with a valued reading WHEN transform THEN period picker kept and sentiment is Content`() {
        // Arrange — a single valued reading for the selected timeframe keeps the section populated
        val indicators = coinIndicators(
            reading(type = Type.GALAXY_SCORE, value = BigDecimal("50"), signal = Signal.POSITIVE),
        )

        // Act
        val result = transform(indicators, prevPeriodPicker = PeriodPickerUM.Loading)

        // Assert — the picker is left untouched, sentiment resolves to Content
        assertThat(result.periodPicker).isEqualTo(PeriodPickerUM.Loading)
        assertThat(result.tokenSentiment).isInstanceOf(TokenSentimentUM.Content::class.java)
    }

    @Test
    fun `GIVEN null indicators WHEN transform THEN period picker kept and sentiment is NoResponse`() {
        // Act — no indicators loaded yet
        val result = transform(coinIndicators = null, prevPeriodPicker = PeriodPickerUM.Loading)

        // Assert — absent indicators must not touch the picker; nothing arrived, so the sentiment is NoResponse
        assertThat(result.periodPicker).isEqualTo(PeriodPickerUM.Loading)
        assertThat(result.tokenSentiment).isEqualTo(TokenSentimentUM.Empty.NoResponse)
    }

    @Test
    fun `GIVEN indicators with no readings WHEN transform THEN picker is Empty and sentiment is NoOutlook`() {
        // Arrange — an empty readings list vacuously satisfies "all values null"
        val indicators = coinIndicators()

        // Act
        val result = transform(indicators, prevPeriodPicker = PeriodPickerUM.Loading)

        // Assert
        assertThat(result.periodPicker).isEqualTo(PeriodPickerUM.Empty)
        assertThat(result.tokenSentiment).isInstanceOf(TokenSentimentUM.Empty.NoOutlook::class.java)
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
        bottomButton = BottomButtonUM.Loading,
        onPeriodClick = {},
        onInfoClick = {},
        onCloseClick = {},
    )

    private fun coinIndicators(vararg readings: CoinIndicators.Reading): CoinIndicators =
        CoinIndicators(symbol = "BTC", readings = readings.toList())

    private fun reading(
        type: Type,
        value: BigDecimal?,
        signal: Signal = Signal.POSITIVE,
    ): CoinIndicators.Reading = CoinIndicators.Reading(
        type = type,
        name = type.name,
        timeframe = Timeframe.DAY,
        value = value,
        signal = signal,
        updatedAt = null,
    )
}