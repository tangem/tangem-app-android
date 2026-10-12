package com.tangem.domain.polymarket.model

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
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
            side = model.side,
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
        val expected: BigDecimal?,
        val side: PredictionOrderSide = PredictionOrderSide.BUY,
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
        MinAmountModel(
            side = PredictionOrderSide.SELL,
            worstCasePrice = BigDecimal("0.90"),
            minOrderSize = BigDecimal("5"),
            minOrderAmount = BigDecimal("1.00"),
            expected = null,
        ),
        MinAmountModel(
            side = PredictionOrderSide.SELL,
            worstCasePrice = BigDecimal("0.10"),
            minOrderSize = BigDecimal("5"),
            minOrderAmount = null,
            expected = null,
        ),
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsPlaceable {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a quote WHEN isPlaceable THEN only a sell lifts the minimum order size`(
            model: PlaceableModel,
        ) {
            // Arrange
            val quote = createQuote(
                side = model.side,
                worstCasePrice = BigDecimal("0.50"),
                minOrderSize = BigDecimal("5"),
                minOrderAmount = null,
                status = model.status,
            )

            // Act
            val actual = quote.isPlaceable

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            PlaceableModel(status = PredictionQuoteStatus.FULL, expected = true),
            PlaceableModel(status = PredictionQuoteStatus.PARTIAL, expected = true),
            PlaceableModel(status = PredictionQuoteStatus.BELOW_MIN_ORDER_SIZE, expected = true),
            PlaceableModel(
                status = PredictionQuoteStatus.BELOW_MIN_ORDER_SIZE,
                side = PredictionOrderSide.BUY,
                expected = false,
            ),
            PlaceableModel(status = PredictionQuoteStatus.INSUFFICIENT_LIQUIDITY, expected = false),
            PlaceableModel(status = PredictionQuoteStatus.MARKET_CLOSED, expected = false),
        )
    }

    internal data class PlaceableModel(
        val status: PredictionQuoteStatus,
        val expected: Boolean,
        val side: PredictionOrderSide = PredictionOrderSide.SELL,
    )

    private fun createQuote(
        side: PredictionOrderSide,
        worstCasePrice: BigDecimal,
        minOrderSize: BigDecimal,
        minOrderAmount: BigDecimal?,
        status: PredictionQuoteStatus = PredictionQuoteStatus.FULL,
    ) = PredictionOrderQuote(
        status = status,
        side = side,
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