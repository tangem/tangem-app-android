package com.tangem.features.polymarket.impl.placeprediction.entity

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PlacePredictionOrderTotalTest {

    @ParameterizedTest
    @ProvideTestModels
    fun orderTotal(model: OrderTotalModel) {
        // Arrange
        val state = PlacePredictionUM.initial(side = model.side).copy(quote = model.quote)

        // Act
        val total = state.orderTotal()

        // Assert
        assertThat(total).isEqualTo(model.expected)
    }

    internal data class OrderTotalModel(
        val side: PredictionOrderSide,
        val quote: QuoteUM,
        val expected: BigDecimal?,
    )

    private fun provideTestModels() = listOf(
        OrderTotalModel(
            side = PredictionOrderSide.BUY,
            quote = content(feeTotal = BigDecimal("1.4"), total = BigDecimal("4001.4")),
            expected = BigDecimal("4001.4"),
        ),
        OrderTotalModel(
            side = PredictionOrderSide.SELL,
            quote = content(feeTotal = BigDecimal("0.45"), total = BigDecimal("43.09")),
            expected = BigDecimal("43.09"),
        ),
        OrderTotalModel(side = PredictionOrderSide.BUY, quote = QuoteUM.Empty, expected = null),
        OrderTotalModel(
            side = PredictionOrderSide.BUY,
            quote = QuoteUM.Loading(isAnswerExpected = false),
            expected = null,
        ),
        OrderTotalModel(
            side = PredictionOrderSide.SELL,
            quote = QuoteUM.Unavailable(status = PredictionQuoteStatus.INSUFFICIENT_LIQUIDITY),
            expected = null,
        ),
        OrderTotalModel(
            side = PredictionOrderSide.BUY,
            quote = QuoteUM.Error(reason = QuoteErrorUM.Network),
            expected = null,
        ),
    )

    private fun content(feeTotal: BigDecimal, total: BigDecimal): QuoteUM.Content = QuoteUM.Content(
        status = PredictionQuoteStatus.FULL,
        expected = total.add(feeTotal),
        feeTotal = feeTotal,
        total = total,
        signable = SignableQuoteUM(quote = mockk(), receivedAtMillis = 0L, assetId = "asset"),
    )
}