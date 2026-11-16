package com.tangem.features.polymarket.impl.placeprediction.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketMarketVersion
import com.tangem.domain.polymarket.model.PredictionOrderFees
import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionOrderState
import com.tangem.domain.polymarket.model.PredictionOrderStatus
import com.tangem.domain.polymarket.model.PredictionPlaceOrderResult
import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import com.tangem.features.polymarket.impl.placeprediction.entity.PlaceResultUM
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PlaceResultUMConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: ConvertModel) {
        // Act
        val actual = convert(result = model.result, side = model.side)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN a partial fill WHEN converted THEN the spent figure is the exchange's own`() {
        // Arrange
        val spent = BigDecimal("13.370000")

        // Act
        val actual = convert(result = result(shares = QUOTED_SHARES.half(), collateral = spent))

        // Assert
        assertThat(actual).isEqualTo(
            PlaceOutcome.Reportable(
                result = PlaceResultUM.PartiallyFilled(
                    placedAt = PLACED_AT,
                    filledCollateral = spent,
                    requestedCollateral = QUOTED_AMOUNT,
                    shares = QUOTED_SHARES.half(),
                    total = BigDecimal("14.07"),
                ),
            ),
        )
    }

    @ParameterizedTest
    @MethodSource("providePartialSellModels")
    fun `GIVEN a partial sell WHEN converted THEN the total is the fill net of its share of the fee`(
        model: PartialSellModel,
    ) {
        // Act
        val actual = convert(
            result = result(side = PredictionOrderSide.SELL, shares = model.shares, collateral = model.received),
            side = PredictionOrderSide.SELL,
        )

        // Assert
        assertThat(actual).isEqualTo(
            PlaceOutcome.Reportable(
                result = PlaceResultUM.PartiallyFilled(
                    placedAt = PLACED_AT,
                    filledCollateral = model.received,
                    requestedCollateral = QUOTED_AMOUNT,
                    shares = model.shares,
                    total = model.expectedTotal,
                ),
            ),
        )
    }

    internal data class PartialSellModel(
        val name: String,
        val shares: BigDecimal,
        val received: BigDecimal,
        val expectedTotal: BigDecimal,
    ) {
        override fun toString(): String = name
    }

    private fun providePartialSellModels() = listOf(
        PartialSellModel(
            name = "half the shares bear half the fee",
            shares = QUOTED_SHARES.half(),
            received = BigDecimal("36.000000"),
            expectedTotal = BigDecimal("35.30"),
        ),
        PartialSellModel(
            name = "a quarter of the shares bear a quarter of the fee",
            shares = BigDecimal("24.489796"),
            received = BigDecimal("18.000000"),
            expectedTotal = BigDecimal("17.65"),
        ),
        PartialSellModel(
            name = "the proceeds round down to cents",
            shares = BigDecimal("32.653061"),
            received = BigDecimal("24.002000"),
            expectedTotal = BigDecimal("23.53"),
        ),
        PartialSellModel(
            name = "a fee above the proceeds leaves nothing",
            shares = BigDecimal("0.979592"),
            received = BigDecimal("0.001000"),
            expectedTotal = BigDecimal("0.00"),
        ),
        PartialSellModel(
            name = "the fill ratio follows the shares, not the collateral",
            shares = QUOTED_SHARES.half(),
            received = BigDecimal("30.000000"),
            expectedTotal = BigDecimal("29.30"),
        ),
    )

    @Test
    fun `GIVEN the collateral leg is short WHEN the shares are whole THEN the fill is full`() {
        // Arrange
        val truncated = QUOTED_AMOUNT - BigDecimal("0.000002")

        // Act
        val actual = convert(result = result(collateral = truncated))

        // Assert
        assertThat(actual).isEqualTo(
            PlaceOutcome.Reportable(result = filled(shares = QUOTED_SHARES).copy(collateralAmount = truncated)),
        )
    }

    @ParameterizedTest
    @EnumSource(PredictionOrderSide::class)
    fun `GIVEN a full fill WHEN converted THEN the actual exchanged amount is shown`(side: PredictionOrderSide) {
        val amount = BigDecimal("70.25")
        val total = when (side) {
            PredictionOrderSide.BUY -> BigDecimal("71.65")
            PredictionOrderSide.SELL -> BigDecimal("68.85")
        }

        val actual = convert(result = result(side = side, collateral = amount), side = side)

        assertThat(actual).isEqualTo(
            PlaceOutcome.Reportable(
                result = filled(shares = QUOTED_SHARES).copy(collateralAmount = amount, total = total),
            ),
        )
    }

    @ParameterizedTest
    @EnumSource(PredictionOrderSide::class)
    fun `GIVEN a full fill without its actual amount WHEN converted THEN the outcome stays unconfirmed`(
        side: PredictionOrderSide,
    ) {
        val actual = convert(result = result(side = side, collateral = null), side = side)

        assertThat(actual).isEqualTo(PlaceOutcome.Reportable(PlaceResultUM.Submitted(placedAt = PLACED_AT)))
    }

    @Test
    fun `GIVEN the book beat the worst case WHEN more shares were filled THEN the fill is full`() {
        // Arrange
        val better = QUOTED_SHARES + BigDecimal("4.5")

        // Act
        val actual = convert(result = result(shares = better))

        // Assert
        assertThat(actual).isEqualTo(PlaceOutcome.Reportable(result = filled(shares = better)))
    }

    @Test
    fun `GIVEN a sell WHEN the money leg is zero THEN the shares still decide the fill`() {
        // Act
        val actual = convert(
            result = result(side = PredictionOrderSide.SELL, collateral = BigDecimal.ZERO),
            side = PredictionOrderSide.SELL,
        )

        // Assert
        assertThat(actual).isEqualTo(
            PlaceOutcome.Reportable(
                result = filled(shares = QUOTED_SHARES).copy(
                    collateralAmount = BigDecimal.ZERO,
                    total = BigDecimal("0.00"),
                ),
            ),
        )
    }

    internal data class ConvertModel(
        val result: PredictionPlaceOrderResult,
        val expected: PlaceOutcome,
        val side: PredictionOrderSide = PredictionOrderSide.BUY,
    )

    private fun provideTestModels() = listOf(
        ConvertModel(
            result = result(),
            expected = PlaceOutcome.Reportable(filled(shares = QUOTED_SHARES)),
        ),
        ConvertModel(
            result = result(status = " MATCHED "),
            expected = PlaceOutcome.Reportable(filled(shares = QUOTED_SHARES)),
        ),
        ConvertModel(
            result = result(shares = BigDecimal("97.9591840")),
            expected = PlaceOutcome.Reportable(filled(shares = BigDecimal("97.9591840"))),
        ),
        ConvertModel(
            result = result(shares = QUOTED_SHARES - BigDecimal("0.000001"), collateral = BigDecimal("71.999999")),
            expected = PlaceOutcome.Reportable(
                PlaceResultUM.PartiallyFilled(
                    placedAt = PLACED_AT,
                    filledCollateral = BigDecimal("71.999999"),
                    requestedCollateral = QUOTED_AMOUNT,
                    shares = QUOTED_SHARES - BigDecimal("0.000001"),
                    total = BigDecimal("73.40"),
                ),
            ),
        ),
        ConvertModel(
            result = result(shares = BigDecimal.ZERO),
            expected = PlaceOutcome.Anomalous(reason = ZERO_SHARES_REASON),
        ),
        ConvertModel(
            result = result(shares = null),
            expected = PlaceOutcome.Reportable(PlaceResultUM.Submitted(placedAt = PLACED_AT)),
        ),
        ConvertModel(
            result = result(shares = QUOTED_SHARES.half(), collateral = null),
            expected = PlaceOutcome.Reportable(PlaceResultUM.Submitted(placedAt = PLACED_AT)),
        ),
        ConvertModel(
            result = result(shares = QUOTED_SHARES + BigDecimal.ONE),
            expected = PlaceOutcome.Reportable(filled(shares = QUOTED_SHARES + BigDecimal.ONE)),
        ),
        ConvertModel(
            result = result(status = "delayed"),
            expected = PlaceOutcome.Delayed(orderId = ORDER_ID, placedAt = PLACED_AT),
        ),
        ConvertModel(
            result = result(status = " Delayed ", shares = null, collateral = null),
            expected = PlaceOutcome.Delayed(orderId = ORDER_ID, placedAt = PLACED_AT),
        ),
        ConvertModel(
            result = result(status = "delayed", orderId = null),
            expected = PlaceOutcome.Reportable(PlaceResultUM.Submitted(placedAt = PLACED_AT)),
        ),
        ConvertModel(
            result = result(status = "delayed", orderId = " "),
            expected = PlaceOutcome.Reportable(PlaceResultUM.Submitted(placedAt = PLACED_AT)),
        ),
        ConvertModel(
            result = result(status = "live"),
            expected = PlaceOutcome.Reportable(PlaceResultUM.Submitted(placedAt = PLACED_AT)),
        ),
        ConvertModel(
            result = result(status = null),
            expected = PlaceOutcome.Reportable(PlaceResultUM.Submitted(placedAt = PLACED_AT)),
        ),
        ConvertModel(
            side = PredictionOrderSide.SELL,
            result = result(side = PredictionOrderSide.SELL),
            expected = PlaceOutcome.Reportable(filled(shares = QUOTED_SHARES).copy(total = SOLD_TOTAL)),
        ),
        ConvertModel(
            side = PredictionOrderSide.SELL,
            result = result(
                side = PredictionOrderSide.SELL,
                shares = QUOTED_SHARES.half(),
                collateral = BigDecimal("36.000000"),
            ),
            expected = PlaceOutcome.Reportable(
                PlaceResultUM.PartiallyFilled(
                    placedAt = PLACED_AT,
                    filledCollateral = BigDecimal("36.000000"),
                    requestedCollateral = QUOTED_AMOUNT,
                    shares = QUOTED_SHARES.half(),
                    total = BigDecimal("35.30"),
                ),
            ),
        ),
        ConvertModel(
            side = PredictionOrderSide.SELL,
            result = result(side = PredictionOrderSide.SELL, shares = BigDecimal.ZERO),
            expected = PlaceOutcome.Anomalous(reason = ZERO_SHARES_REASON),
        ),
        ConvertModel(
            side = PredictionOrderSide.SELL,
            result = result(side = PredictionOrderSide.SELL, shares = null),
            expected = PlaceOutcome.Reportable(PlaceResultUM.Submitted(placedAt = PLACED_AT)),
        ),
        ConvertModel(
            side = PredictionOrderSide.SELL,
            result = result(side = PredictionOrderSide.SELL, shares = QUOTED_SHARES.half(), collateral = null),
            expected = PlaceOutcome.Reportable(PlaceResultUM.Submitted(placedAt = PLACED_AT)),
        ),
        ConvertModel(
            side = PredictionOrderSide.SELL,
            result = result(side = PredictionOrderSide.SELL, shares = QUOTED_SHARES + BigDecimal.ONE),
            expected = PlaceOutcome.Reportable(filled(shares = QUOTED_SHARES + BigDecimal.ONE).copy(total = SOLD_TOTAL)),
        ),
    )

    @ParameterizedTest
    @MethodSource("provideReadBackModels")
    fun convertReadBack(model: ReadBackModel) {
        // Act
        val actual = PlaceResultUMConverter.convert(
            order = model.order,
            quote = model.quote,
            placedAt = PLACED_AT,
        )

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class ReadBackModel(
        val name: String,
        val order: PredictionOrderState,
        val expected: PlaceOutcome,
        val quote: PredictionOrderQuote = QUOTE,
    ) {
        override fun toString(): String = name
    }

    private fun provideReadBackModels() = listOf(
        ReadBackModel(
            name = "the whole size matched is a full fill spent at the order's price",
            order = readBack(sizeMatched = ORDERED_SHARES),
            expected = PlaceOutcome.Reportable(
                result = PlaceResultUM.Filled(
                    placedAt = PLACED_AT,
                    collateralAmount = BigDecimal("73.50"),
                    shares = ORDERED_SHARES,
                    fee = QUOTED_FEE,
                    total = BigDecimal("74.90"),
                ),
            ),
        ),
        ReadBackModel(
            name = "more than the size is still a full fill",
            order = readBack(sizeMatched = ORDERED_SHARES + BigDecimal.ONE),
            expected = PlaceOutcome.Reportable(
                result = PlaceResultUM.Filled(
                    placedAt = PLACED_AT,
                    collateralAmount = BigDecimal("74.24"),
                    shares = ORDERED_SHARES + BigDecimal.ONE,
                    fee = QUOTED_FEE,
                    total = BigDecimal("75.64"),
                ),
            ),
        ),
        ReadBackModel(
            name = "a partial fill is spent at the order's price, half up to cents",
            order = readBack(sizeMatched = BigDecimal("33.333333")),
            expected = PlaceOutcome.Reportable(
                result = PlaceResultUM.PartiallyFilled(
                    placedAt = PLACED_AT,
                    filledCollateral = BigDecimal("24.50"),
                    requestedCollateral = QUOTED_AMOUNT,
                    shares = BigDecimal("33.333333"),
                    total = BigDecimal("24.98"),
                ),
            ),
        ),
        ReadBackModel(
            name = "a partial sell nets its share of the fee from the proceeds",
            order = readBack(sizeMatched = QUOTED_SHARES.half()),
            quote = SELL_QUOTE,
            expected = PlaceOutcome.Reportable(
                result = PlaceResultUM.PartiallyFilled(
                    placedAt = PLACED_AT,
                    filledCollateral = BigDecimal("36.00"),
                    requestedCollateral = QUOTED_AMOUNT,
                    shares = QUOTED_SHARES.half(),
                    total = BigDecimal("35.30"),
                ),
            ),
        ),
        ReadBackModel(
            name = "without quoted shares the fee is shared by the collateral filled",
            order = readBack(sizeMatched = QUOTED_SHARES.half()),
            quote = SELL_QUOTE.copy(shares = BigDecimal.ZERO),
            expected = PlaceOutcome.Reportable(
                result = PlaceResultUM.PartiallyFilled(
                    placedAt = PLACED_AT,
                    filledCollateral = BigDecimal("36.00"),
                    requestedCollateral = QUOTED_AMOUNT,
                    shares = QUOTED_SHARES.half(),
                    total = BigDecimal("35.30"),
                ),
            ),
        ),
        ReadBackModel(
            name = "a matched order with nothing filled is an anomaly",
            order = readBack(sizeMatched = BigDecimal.ZERO),
            expected = PlaceOutcome.Anomalous(reason = ZERO_SHARES_REASON),
        ),
        ReadBackModel(
            name = "a fill whose size cannot be read is no fill",
            order = readBack(sizeMatched = null),
            expected = PlaceOutcome.Anomalous(reason = ZERO_SHARES_REASON),
        ),
        ReadBackModel(
            name = "an order whose size cannot be read is an anomaly",
            order = readBack(sizeMatched = BigDecimal.TEN, originalSize = null),
            expected = PlaceOutcome.Anomalous(reason = UNREADABLE_FILL_REASON),
        ),
        ReadBackModel(
            name = "a fill without a price cannot be spent and is an anomaly",
            order = readBack(sizeMatched = BigDecimal.TEN, price = null),
            expected = PlaceOutcome.Anomalous(reason = UNREADABLE_FILL_REASON),
        ),
    )

    private fun convert(
        result: PredictionPlaceOrderResult,
        side: PredictionOrderSide = PredictionOrderSide.BUY,
    ) = PlaceResultUMConverter.convert(
        result = result,
        side = side,
        quote = QUOTE,
        placedAt = PLACED_AT,
    )

    private companion object {
        val QUOTED_AMOUNT: BigDecimal = BigDecimal("72.000000")
        val QUOTED_SHARES: BigDecimal = BigDecimal("97.959184")
        val QUOTED_FEE: BigDecimal = BigDecimal("1.40")
        val QUOTED_TOTAL: BigDecimal = BigDecimal("73.40")
        val SOLD_TOTAL: BigDecimal = BigDecimal("70.60")
        val QUOTE = PredictionOrderQuote(
            status = PredictionQuoteStatus.FULL,
            side = PredictionOrderSide.BUY,
            shares = QUOTED_SHARES,
            notional = QUOTED_AMOUNT,
            expectedExecutionAmount = QUOTED_SHARES,
            averagePrice = BigDecimal("0.735"),
            worstCasePrice = BigDecimal("0.74"),
            fees = PredictionOrderFees(market = BigDecimal("1.00"), builder = BigDecimal("0.40"), total = QUOTED_FEE),
            total = QUOTED_TOTAL,
            builderCode = "0xbuilder",
            minOrderSize = BigDecimal("5"),
            minOrderAmount = BigDecimal.ONE,
            tickSize = BigDecimal("0.001"),
            isLive = false,
            version = PolymarketMarketVersion.V1,
            exchange = null,
            signingDomainVersion = null,
        )
        val SELL_QUOTE = QUOTE.copy(side = PredictionOrderSide.SELL)
        const val PLACED_AT = 1_787_300_000_000L
        const val ZERO_SHARES_REASON = "The exchange matched the order and filled no shares"
        const val UNREADABLE_FILL_REASON = "The exchange matched the order, but its fill cannot be read"

        fun BigDecimal.half(): BigDecimal = divide(BigDecimal("2"))

        fun filled(shares: BigDecimal) = PlaceResultUM.Filled(
            placedAt = PLACED_AT,
            collateralAmount = QUOTED_AMOUNT,
            shares = shares,
            fee = QUOTED_FEE,
            total = QUOTED_TOTAL,
        )

        const val ORDER_ID = "0x6b2e"
        val ORDERED_SHARES: BigDecimal = BigDecimal("100")
        val LIMIT_PRICE: BigDecimal = BigDecimal("0.735")

        fun readBack(
            sizeMatched: BigDecimal?,
            originalSize: BigDecimal? = ORDERED_SHARES,
            price: BigDecimal? = LIMIT_PRICE,
        ) = PredictionOrderState(
            status = PredictionOrderStatus.Matched,
            price = price,
            originalSize = originalSize,
            sizeMatched = sizeMatched,
        )

        fun result(
            status: String? = "matched",
            shares: BigDecimal? = QUOTED_SHARES,
            collateral: BigDecimal? = QUOTED_AMOUNT,
            orderId: String? = ORDER_ID,
            side: PredictionOrderSide = PredictionOrderSide.BUY,
        ) = PredictionPlaceOrderResult(
            isSuccess = true,
            orderId = orderId,
            status = status,
            makingAmount = if (side == PredictionOrderSide.BUY) collateral else shares,
            takingAmount = if (side == PredictionOrderSide.BUY) shares else collateral,
            transactionHashes = listOf("0xdead"),
            errorMessage = null,
        )
    }
}