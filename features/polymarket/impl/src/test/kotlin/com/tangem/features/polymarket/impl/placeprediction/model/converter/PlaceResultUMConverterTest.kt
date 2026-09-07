package com.tangem.features.polymarket.impl.placeprediction.model.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PredictionPlaceOrderResult
import com.tangem.features.polymarket.impl.placeprediction.entity.PlaceResultUM
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PlaceResultUMConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: ConvertModel) {
        // Act
        val actual = convert(result = model.result)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN a partial fill WHEN converted THEN the spent figure is the exchange's own`() {
        // Arrange
        val spent = BigDecimal("13.370000")

        // Act
        val actual = convert(result = result(shares = QUOTED_SHARES.half(), makingAmount = spent))

        // Assert
        assertThat(actual).isEqualTo(
            PlaceOutcome.Reportable(
                result = PlaceResultUM.PartiallyFilled(
                    placedAt = PLACED_AT,
                    filledAmount = spent,
                    requestedAmount = QUOTED_AMOUNT,
                    tokensBought = QUOTED_SHARES.half(),
                ),
            ),
        )
    }

    @Test
    fun `GIVEN the collateral leg is short WHEN the shares are whole THEN the fill is full`() {
        // Arrange
        val truncated = QUOTED_AMOUNT - BigDecimal("0.000002")

        // Act
        val actual = convert(result = result(makingAmount = truncated))

        // Assert
        assertThat(actual).isEqualTo(PlaceOutcome.Reportable(result = filled(tokensBought = QUOTED_SHARES)))
    }

    internal data class ConvertModel(val result: PredictionPlaceOrderResult, val expected: PlaceOutcome)

    private fun provideTestModels() = listOf(
        ConvertModel(
            result = result(),
            expected = PlaceOutcome.Reportable(filled(tokensBought = QUOTED_SHARES)),
        ),
        ConvertModel(
            result = result(status = " MATCHED "),
            expected = PlaceOutcome.Reportable(filled(tokensBought = QUOTED_SHARES)),
        ),
        ConvertModel(
            result = result(shares = BigDecimal("97.9591840")),
            expected = PlaceOutcome.Reportable(filled(tokensBought = BigDecimal("97.9591840"))),
        ),
        ConvertModel(
            result = result(shares = QUOTED_SHARES - BigDecimal("0.000001"), makingAmount = BigDecimal("71.999999")),
            expected = PlaceOutcome.Reportable(
                PlaceResultUM.PartiallyFilled(
                    placedAt = PLACED_AT,
                    filledAmount = BigDecimal("71.999999"),
                    requestedAmount = QUOTED_AMOUNT,
                    tokensBought = QUOTED_SHARES - BigDecimal("0.000001"),
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
            result = result(shares = QUOTED_SHARES.half(), makingAmount = null),
            expected = PlaceOutcome.Reportable(PlaceResultUM.Submitted(placedAt = PLACED_AT)),
        ),
        ConvertModel(
            result = result(shares = QUOTED_SHARES + BigDecimal.ONE),
            expected = PlaceOutcome.Reportable(PlaceResultUM.Submitted(placedAt = PLACED_AT)),
        ),
        ConvertModel(
            result = result(status = "delayed"),
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
    )

    private fun convert(result: PredictionPlaceOrderResult) = PlaceResultUMConverter.convert(
        result = result,
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

        fun filled(tokensBought: BigDecimal) = PlaceResultUM.Filled(
            placedAt = PLACED_AT,
            amount = QUOTED_AMOUNT,
            tokensBought = tokensBought,
        )

        fun result(
            status: String? = "matched",
            shares: BigDecimal? = QUOTED_SHARES,
            makingAmount: BigDecimal? = QUOTED_AMOUNT,
        ) = PredictionPlaceOrderResult(
            isSuccess = true,
            orderId = "0x6b2e",
            status = status,
            makingAmount = makingAmount,
            takingAmount = shares,
            transactionHashes = listOf("0xdead"),
            errorMessage = null,
        )
    }
}