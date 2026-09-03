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
        val actual = PlaceResultUMConverter.convert(
            result = model.result,
            requestedAmount = REQUESTED,
            placedAt = PLACED_AT,
        )

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN a partial fill WHEN converted THEN the filled figure is the exchange's own`() {
        // Arrange
        val reported = BigDecimal("13.370000")

        // Act
        val actual = PlaceResultUMConverter.convert(
            result = result(makingAmount = reported),
            requestedAmount = REQUESTED,
            placedAt = PLACED_AT,
        )

        // Assert
        assertThat(actual).isEqualTo(
            PlaceResultUM.PartiallyFilled(
                placedAt = PLACED_AT,
                filledAmount = reported,
                requestedAmount = REQUESTED,
                tokensBought = TAKING,
            ),
        )
    }

    internal data class ConvertModel(val result: PredictionPlaceOrderResult, val expected: PlaceResultUM)

    private fun provideTestModels() = listOf(
        ConvertModel(
            result = result(makingAmount = REQUESTED),
            expected = PlaceResultUM.Filled(placedAt = PLACED_AT, amount = REQUESTED, tokensBought = TAKING),
        ),
        ConvertModel(
            result = result(status = " MATCHED "),
            expected = PlaceResultUM.Filled(placedAt = PLACED_AT, amount = REQUESTED, tokensBought = TAKING),
        ),
        ConvertModel(
            result = result(makingAmount = REQUESTED - BigDecimal("0.000001")),
            expected = PlaceResultUM.Filled(placedAt = PLACED_AT, amount = REQUESTED, tokensBought = TAKING),
        ),
        ConvertModel(
            result = result(makingAmount = REQUESTED - BigDecimal("0.000002")),
            expected = PlaceResultUM.PartiallyFilled(
                placedAt = PLACED_AT,
                filledAmount = REQUESTED - BigDecimal("0.000002"),
                requestedAmount = REQUESTED,
                tokensBought = TAKING,
            ),
        ),
        ConvertModel(
            result = result(makingAmount = BigDecimal("36.000000")),
            expected = PlaceResultUM.PartiallyFilled(
                placedAt = PLACED_AT,
                filledAmount = BigDecimal("36.000000"),
                requestedAmount = REQUESTED,
                tokensBought = TAKING,
            ),
        ),
        ConvertModel(
            result = result(makingAmount = BigDecimal.ZERO),
            expected = PlaceResultUM.NotFilled(placedAt = PLACED_AT),
        ),
        ConvertModel(
            result = result(makingAmount = null),
            expected = PlaceResultUM.Submitted(placedAt = PLACED_AT),
        ),
        ConvertModel(
            result = result(status = "delayed", makingAmount = REQUESTED),
            expected = PlaceResultUM.Submitted(placedAt = PLACED_AT),
        ),
        ConvertModel(
            result = result(status = "live"),
            expected = PlaceResultUM.Submitted(placedAt = PLACED_AT),
        ),
        ConvertModel(
            result = result(status = null),
            expected = PlaceResultUM.Submitted(placedAt = PLACED_AT),
        ),
        ConvertModel(
            result = result(makingAmount = BigDecimal("72000000")),
            expected = PlaceResultUM.Submitted(placedAt = PLACED_AT),
        ),
    )

    private companion object {
        val REQUESTED: BigDecimal = BigDecimal("72.000000")
        val TAKING: BigDecimal = BigDecimal("97.959184")
        const val PLACED_AT = 1_787_300_000_000L

        fun result(
            status: String? = "matched",
            makingAmount: BigDecimal? = REQUESTED,
        ) = PredictionPlaceOrderResult(
            isSuccess = true,
            orderId = "0x6b2e",
            status = status,
            makingAmount = makingAmount,
            takingAmount = TAKING,
            transactionHashes = listOf("0xdead"),
            errorMessage = null,
        )
    }
}