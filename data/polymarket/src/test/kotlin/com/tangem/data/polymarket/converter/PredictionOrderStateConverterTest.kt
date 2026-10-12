package com.tangem.data.polymarket.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PredictionOrderState
import com.tangem.domain.polymarket.model.PredictionOrderStatus
import com.tangem.grow.datasource.polymarket.clob.models.PolymarketOrderResponse
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PredictionOrderStateConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun status(model: StatusModel) {
        // Act
        val actual = PredictionOrderStateConverter.convert(response(status = model.raw))

        // Assert
        assertThat(actual.status).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN base-unit sizes WHEN converted THEN sizes are read as shares and the price as it is`() {
        // Act
        val actual = PredictionOrderStateConverter.convert(
            response(
                status = "ORDER_STATUS_MATCHED",
                price = "0.52",
                originalSize = "100000000",
                sizeMatched = "37500000",
            ),
        )

        // Assert
        assertThat(actual).isEqualTo(
            PredictionOrderState(
                status = PredictionOrderStatus.Matched,
                price = BigDecimal("0.52"),
                originalSize = BigDecimal("100.000000"),
                sizeMatched = BigDecimal("37.500000"),
            ),
        )
    }

    @Test
    fun `GIVEN unreadable numbers WHEN converted THEN they are absent rather than a failure`() {
        // Act
        val actual = PredictionOrderStateConverter.convert(
            response(status = "ORDER_STATUS_MATCHED", price = "n/a", originalSize = null, sizeMatched = ""),
        )

        // Assert
        assertThat(actual).isEqualTo(
            PredictionOrderState(
                status = PredictionOrderStatus.Matched,
                price = null,
                originalSize = null,
                sizeMatched = null,
            ),
        )
    }

    internal data class StatusModel(val raw: String?, val expected: PredictionOrderStatus)

    private fun provideTestModels() = listOf(
        StatusModel(raw = "ORDER_STATUS_MATCHED", expected = PredictionOrderStatus.Matched),
        StatusModel(raw = " ORDER_STATUS_MATCHED ", expected = PredictionOrderStatus.Matched),
        StatusModel(raw = "ORDER_STATUS_LIVE", expected = PredictionOrderStatus.Other(raw = "ORDER_STATUS_LIVE")),
        StatusModel(
            raw = "ORDER_STATUS_CANCELED",
            expected = PredictionOrderStatus.Other(raw = "ORDER_STATUS_CANCELED"),
        ),
        StatusModel(raw = "matched", expected = PredictionOrderStatus.Other(raw = "matched")),
        StatusModel(raw = null, expected = PredictionOrderStatus.Other(raw = null)),
    )

    private fun response(
        status: String?,
        price: String? = "0.5",
        originalSize: String? = "10000000",
        sizeMatched: String? = "0",
    ) = PolymarketOrderResponse(
        id = "0x6b2e",
        status = status,
        price = price,
        originalSize = originalSize,
        sizeMatched = sizeMatched,
    )
}