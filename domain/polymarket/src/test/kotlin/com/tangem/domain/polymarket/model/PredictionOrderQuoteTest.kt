package com.tangem.domain.polymarket.model

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PredictionOrderQuoteTest {

    @ParameterizedTest
    @ProvideTestModels
    fun minAmount(model: MinAmountModel) {
        // Arrange
        val quote = createQuote(
            worstCasePrice = model.worstCasePrice,
            minOrderSize = model.minOrderSize,
            minOrderAmount = model.minOrderAmount,
        )

        // Act
        val actual = quote.minAmount

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class MinAmountModel(
        val worstCasePrice: BigDecimal,
        val minOrderSize: BigDecimal,
        val minOrderAmount: BigDecimal?,
        val expected: BigDecimal,
    )

    private fun provideTestModels() = listOf(
        MinAmountModel(
            worstCasePrice = BigDecimal("0.90"),
            minOrderSize = BigDecimal("5"),
            minOrderAmount = BigDecimal("1.00"),
            expected = BigDecimal("4.50"),
        ),
        MinAmountModel(
            worstCasePrice = BigDecimal("0.10"),
            minOrderSize = BigDecimal("5"),
            minOrderAmount = BigDecimal("1.00"),
            expected = BigDecimal("1.00"),
        ),
        MinAmountModel(
            worstCasePrice = BigDecimal("0.10"),
            minOrderSize = BigDecimal("5"),
            minOrderAmount = null,
            expected = BigDecimal("0.50"),
        ),
    )

    private fun createQuote(
        worstCasePrice: BigDecimal,
        minOrderSize: BigDecimal,
        minOrderAmount: BigDecimal?,
    ) = PredictionOrderQuote(
        status = PredictionQuoteStatus.FULL,
        side = PredictionOrderSide.BUY,
        shares = BigDecimal("10"),
        notional = BigDecimal("5"),
        expectedExecutionAmount = BigDecimal("10"),
        averagePrice = BigDecimal("0.5"),
        worstCasePrice = worstCasePrice,
        fees = PredictionOrderFees(market = BigDecimal.ZERO, builder = BigDecimal.ZERO, total = BigDecimal.ZERO),
        total = BigDecimal("5"),
        builderCode = "0xbuilder",
        minOrderSize = minOrderSize,
        minOrderAmount = minOrderAmount,
        tickSize = BigDecimal("0.001"),
        isLive = false,
    )
}