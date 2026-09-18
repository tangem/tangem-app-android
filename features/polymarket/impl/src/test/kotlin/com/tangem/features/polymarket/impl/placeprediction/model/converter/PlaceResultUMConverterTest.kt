package com.tangem.features.polymarket.impl.placeprediction.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionOrderState
import com.tangem.domain.polymarket.model.PredictionOrderStatus
import com.tangem.domain.polymarket.model.PredictionPlaceOrderResult
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
                ),
            ),
        )
    }

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

        val actual = convert(result = result(side = side, collateral = amount), side = side)

        assertThat(actual).isEqualTo(
            PlaceOutcome.Reportable(result = filled(shares = QUOTED_SHARES).copy(collateralAmount = amount)),
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
            PlaceOutcome.Reportable(result = filled(shares = QUOTED_SHARES).copy(collateralAmount = BigDecimal.ZERO)),
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
            expected = PlaceOutcome.Reportable(filled(shares = QUOTED_SHARES)),
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
            expected = PlaceOutcome.Reportable(filled(shares = QUOTED_SHARES + BigDecimal.ONE)),
        ),
    )

    @ParameterizedTest
    @MethodSource("provideReadBackModels")
    fun convertReadBack(model: ReadBackModel) {
        // Act
        val actual = PlaceResultUMConverter.convert(
            order = model.order,
            quotedAmount = QUOTED_AMOUNT,
            placedAt = PLACED_AT,
        )

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class ReadBackModel(val name: String, val order: PredictionOrderState, val expected: PlaceResultUM?) {
        override fun toString(): String = name
    }

    private fun provideReadBackModels() = listOf(
        ReadBackModel(
            name = "the whole size matched is a full fill spent at the order's price",
            order = readBack(sizeMatched = ORDERED_SHARES),
            expected = PlaceResultUM.Filled(
                placedAt = PLACED_AT,
                collateralAmount = BigDecimal("73.50"),
                shares = ORDERED_SHARES,
            ),
        ),
        ReadBackModel(
            name = "more than the size is still a full fill",
            order = readBack(sizeMatched = ORDERED_SHARES + BigDecimal.ONE),
            expected = PlaceResultUM.Filled(
                placedAt = PLACED_AT,
                collateralAmount = BigDecimal("74.24"),
                shares = ORDERED_SHARES + BigDecimal.ONE,
            ),
        ),
        ReadBackModel(
            name = "a partial fill is spent at the order's price, half up to cents",
            order = readBack(sizeMatched = BigDecimal("33.333333")),
            expected = PlaceResultUM.PartiallyFilled(
                placedAt = PLACED_AT,
                filledCollateral = BigDecimal("24.50"),
                requestedCollateral = QUOTED_AMOUNT,
                shares = BigDecimal("33.333333"),
            ),
        ),
        ReadBackModel(
            name = "a matched order with nothing filled is no fill",
            order = readBack(sizeMatched = BigDecimal.ZERO),
            expected = PlaceResultUM.NotFilled(placedAt = PLACED_AT),
        ),
        ReadBackModel(
            name = "a fill whose size cannot be read has no result",
            order = readBack(sizeMatched = null),
            expected = null,
        ),
        ReadBackModel(
            name = "an order whose size cannot be read has no result",
            order = readBack(sizeMatched = BigDecimal.TEN, originalSize = null),
            expected = null,
        ),
        ReadBackModel(
            name = "a fill without a price cannot be spent and has no result",
            order = readBack(sizeMatched = BigDecimal.TEN, price = null),
            expected = null,
        ),
    )

    private fun convert(
        result: PredictionPlaceOrderResult,
        side: PredictionOrderSide = PredictionOrderSide.BUY,
    ) = PlaceResultUMConverter.convert(
        result = result,
        side = side,
        quotedShares = QUOTED_SHARES,
        quotedAmount = QUOTED_AMOUNT,
        placedAt = PLACED_AT,
    )

    private companion object {
        val QUOTED_AMOUNT: BigDecimal = BigDecimal("72.000000")
        val QUOTED_SHARES: BigDecimal = BigDecimal("97.959184")
        const val PLACED_AT = 1_787_300_000_000L
        const val ZERO_SHARES_REASON = "The exchange matched the order and filled no shares"

        fun BigDecimal.half(): BigDecimal = divide(BigDecimal("2"))

        fun filled(shares: BigDecimal) = PlaceResultUM.Filled(
            placedAt = PLACED_AT,
            collateralAmount = QUOTED_AMOUNT,
            shares = shares,
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