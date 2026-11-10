package com.tangem.data.polymarket.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PredictionPlaceOrderResult
import com.tangem.grow.datasource.polymarket.clob.models.PolymarketPlaceOrderResponse
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PredictionPlaceOrderConverterTest {

    @Test
    fun `GIVEN a matched order WHEN converted THEN its amounts are read as they are`() {
        // Act
        val actual = PredictionPlaceOrderConverter.convert(
            response(status = "matched", makingAmount = "10.5", takingAmount = "21"),
        )

        // Assert
        assertThat(actual).isEqualTo(
            PredictionPlaceOrderResult(
                isSuccess = true,
                orderId = ORDER_ID,
                status = "matched",
                makingAmount = BigDecimal("10.5"),
                takingAmount = BigDecimal("21"),
                transactionHashes = listOf("0xhash"),
                errorMessage = "",
            ),
        )
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN amounts that are not numbers WHEN converted THEN they are absent rather than a failure`(
        model: UnreadableAmountModel,
    ) {
        // Act
        val actual = PredictionPlaceOrderConverter.convert(
            response(status = "delayed", makingAmount = model.raw, takingAmount = model.raw),
        )

        // Assert
        assertThat(actual).isEqualTo(
            PredictionPlaceOrderResult(
                isSuccess = true,
                orderId = ORDER_ID,
                status = "delayed",
                makingAmount = null,
                takingAmount = null,
                transactionHashes = listOf("0xhash"),
                errorMessage = "",
            ),
        )
    }

    internal data class UnreadableAmountModel(val raw: String?)

    private fun provideTestModels() = listOf(
        UnreadableAmountModel(raw = ""),
        UnreadableAmountModel(raw = "n/a"),
        UnreadableAmountModel(raw = null),
    )

    private fun response(status: String?, makingAmount: String?, takingAmount: String?) =
        PolymarketPlaceOrderResponse(
            isSuccess = true,
            orderId = ORDER_ID,
            status = status,
            makingAmount = makingAmount,
            takingAmount = takingAmount,
            transactionHashes = listOf("0xhash"),
            errorMessage = "",
        )

    private companion object {
        const val ORDER_ID = "0x6b2e"
    }
}