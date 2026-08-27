package com.tangem.data.polymarket.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.polymarket.models.PolymarketOrderQuoteFeesResponse
import com.tangem.datasource.api.polymarket.models.PolymarketOrderQuoteRequest
import com.tangem.datasource.api.polymarket.models.PolymarketOrderQuoteResponse
import com.tangem.domain.polymarket.model.PredictionOrderFees
import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.domain.polymarket.model.PredictionOrderQuoteRequest
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PredictionOrderQuoteConverterTest {

    @Test
    fun `GIVEN response with exponential notation WHEN convert THEN parsed as BigDecimal`() {
        // Arrange
        val response = createResponse(notional = "4.07E9", status = "PARTIAL")

        // Act
        val actual = PredictionOrderQuoteConverter.convert(value = response)

        // Assert
        assertThat(actual.notional).isEqualTo(BigDecimal("4.07E9"))
        assertThat(actual.status).isEqualTo(PredictionQuoteStatus.PARTIAL)
    }

    @Test
    fun `GIVEN full response WHEN convert THEN every field mapped`() {
        // Arrange
        val response = createResponse()

        // Act
        val actual = PredictionOrderQuoteConverter.convert(value = response)

        // Assert
        assertThat(actual).isEqualTo(
            PredictionOrderQuote(
                status = PredictionQuoteStatus.FULL,
                side = PredictionOrderSide.BUY,
                shares = BigDecimal("674.76383"),
                notional = BigDecimal("500.00"),
                expectedExecutionAmount = BigDecimal("684.93150"),
                averagePrice = BigDecimal("0.730"),
                worstCasePrice = BigDecimal("0.741"),
                fees = PredictionOrderFees(
                    market = BigDecimal("6.75000"),
                    builder = BigDecimal("12.50000"),
                    total = BigDecimal("19.25000"),
                ),
                total = BigDecimal("519.25000"),
                builderCode = "0xbuilder",
                minOrderSize = BigDecimal("5"),
                tickSize = BigDecimal("0.001"),
                isLive = true,
            ),
        )
    }

    @ParameterizedTest
    @ProvideTestModels
    fun convertStatus(model: StatusModel) {
        // Arrange
        val response = createResponse(status = model.wireStatus)

        // Act
        val actual = PredictionOrderQuoteConverter.convert(value = response)

        // Assert
        assertThat(actual.status).isEqualTo(model.expected)
    }

    internal data class StatusModel(val wireStatus: String, val expected: PredictionQuoteStatus)

    private fun provideTestModels() = listOf(
        StatusModel(wireStatus = "FULL", expected = PredictionQuoteStatus.FULL),
        StatusModel(wireStatus = "PARTIAL", expected = PredictionQuoteStatus.PARTIAL),
        StatusModel(wireStatus = "INSUFFICIENT_LIQUIDITY", expected = PredictionQuoteStatus.INSUFFICIENT_LIQUIDITY),
        StatusModel(wireStatus = "BELOW_MIN_ORDER_SIZE", expected = PredictionQuoteStatus.BELOW_MIN_ORDER_SIZE),
        StatusModel(wireStatus = "MARKET_CLOSED", expected = PredictionQuoteStatus.MARKET_CLOSED),
        StatusModel(wireStatus = "SOMETHING_NEW", expected = PredictionQuoteStatus.MARKET_CLOSED),
        StatusModel(wireStatus = "full", expected = PredictionQuoteStatus.MARKET_CLOSED),
        StatusModel(wireStatus = "", expected = PredictionQuoteStatus.MARKET_CLOSED),
    )

    @Test
    fun `GIVEN a status that forbids placing WHEN converted THEN its zeroes are carried as they came`() {
        // Arrange — the contract zeroes every figure for these statuses, so nothing here may be rendered
        val response = createResponse(
            status = "INSUFFICIENT_LIQUIDITY",
            shares = "0",
            notional = "0",
            expectedExecutionAmount = "0",
            averagePrice = "0",
            worstCasePrice = "0",
            total = "0",
        )

        // Act
        val actual = PredictionOrderQuoteConverter.convert(value = response)

        // Assert
        assertThat(actual.status.isPlaceable).isFalse()
        assertThat(actual.worstCasePrice).isEqualTo(BigDecimal.ZERO)
        assertThat(actual.total).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `GIVEN a status this build does not know WHEN converted THEN it is unplaceable rather than a full fill`() {
        // Act
        val actual = PredictionOrderQuoteConverter.convert(value = createResponse(status = "SOMETHING_NEW"))

        // Assert
        assertThat(actual.status.isPlaceable).isFalse()
    }

    @Test
    fun `GIVEN an unknown status priced by the exchange WHEN converted THEN its figures are not zeroes`() {
        // Arrange — the zeroes belong to the statuses the exchange refuses with, not to the one we substitute
        val response = createResponse(status = "SOMETHING_NEW", total = "519.25000")

        // Act
        val actual = PredictionOrderQuoteConverter.convert(value = response)

        // Assert
        assertThat(actual.status).isEqualTo(PredictionQuoteStatus.MARKET_CLOSED)
        assertThat(actual.total).isEqualTo(BigDecimal("519.25000"))
    }

    @Test
    fun `GIVEN request WHEN toRequestBody THEN amounts sent as plain strings`() {
        // Arrange
        val request = PredictionOrderQuoteRequest(
            marketId = "2944989",
            assetId = "1116047",
            side = PredictionOrderSide.BUY,
            amount = BigDecimal("1E+3"),
            slippagePercent = BigDecimal("0.25"),
        )

        // Act
        val actual = PredictionOrderQuoteConverter.toRequestBody(request = request)

        // Assert — the whole body, so a swapped market and outcome cannot pass unnoticed: it would quote
        // the opposite bet rather than fail
        assertThat(actual).isEqualTo(
            PolymarketOrderQuoteRequest(
                marketId = "2944989",
                assetId = "1116047",
                side = "BUY",
                amount = "1000",
                slippage = "0.25",
            ),
        )
    }

    @Test
    fun `GIVEN no slippage WHEN toRequestBody THEN slippage omitted`() {
        // Arrange
        val request = PredictionOrderQuoteRequest(
            marketId = "2944989",
            assetId = "1116047",
            side = PredictionOrderSide.SELL,
            amount = BigDecimal("12.5"),
            slippagePercent = null,
        )

        // Act
        val actual = PredictionOrderQuoteConverter.toRequestBody(request = request)

        // Assert
        assertThat(actual.slippage).isNull()
    }

    private fun createResponse(
        status: String = "FULL",
        shares: String = "674.76383",
        notional: String = "500.00",
        expectedExecutionAmount: String = "684.93150",
        averagePrice: String = "0.730",
        worstCasePrice: String = "0.741",
        total: String = "519.25000",
        builderCode: String = "0xbuilder",
        minOrderSize: String = "5",
        tickSize: String = "0.001",
        isLive: Boolean = true,
    ): PolymarketOrderQuoteResponse = PolymarketOrderQuoteResponse(
        status = status,
        side = "BUY",
        shares = shares,
        notional = notional,
        expectedExecutionAmount = expectedExecutionAmount,
        averagePrice = averagePrice,
        worstCasePrice = worstCasePrice,
        fees = PolymarketOrderQuoteFeesResponse(
            market = "6.75000",
            builder = "12.50000",
            total = "19.25000",
        ),
        total = total,
        builderCode = builderCode,
        minOrderSize = minOrderSize,
        tickSize = tickSize,
        isLive = isLive,
    )
}