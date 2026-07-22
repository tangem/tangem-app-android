package com.tangem.data.yield.supply.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.tangemTech.models.YieldTokenChartResponse
import com.tangem.domain.yield.supply.models.YieldSupplyMarketChartData
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class YieldTokenChartConverterTest {

    @Test
    fun `GIVEN response with data points WHEN convert THEN splits avgApy into y and bucketIndex into x preserving order`() {
        // Arrange
        val response = response(
            averageApy = BigDecimal("4.25"),
            points = listOf(
                YieldTokenChartResponse.DataPoint(bucketIndex = 0, avgApy = BigDecimal("3.5")),
                YieldTokenChartResponse.DataPoint(bucketIndex = 1, avgApy = BigDecimal("4.0")),
                YieldTokenChartResponse.DataPoint(bucketIndex = 2, avgApy = BigDecimal("5.0")),
            ),
        )

        // Act
        val result = YieldTokenChartConverter.convert(response)

        // Assert
        assertThat(result).isEqualTo(
            YieldSupplyMarketChartData(
                y = listOf(3.5, 4.0, 5.0),
                x = listOf(0.0, 1.0, 2.0),
                avr = 4.25,
            ),
        )
    }

    @Test
    fun `GIVEN response with empty data WHEN convert THEN returns empty y and x with average`() {
        // Arrange
        val response = response(averageApy = BigDecimal("1.0"), points = emptyList())

        // Act
        val result = YieldTokenChartConverter.convert(response)

        // Assert
        assertThat(result).isEqualTo(
            YieldSupplyMarketChartData(y = emptyList(), x = emptyList(), avr = 1.0),
        )
    }

    private fun response(averageApy: BigDecimal, points: List<YieldTokenChartResponse.DataPoint>) =
        YieldTokenChartResponse(
            underlying = "USDT",
            market = "aave",
            bucketSizeDays = 1,
            period = "30d",
            data = points,
            averageApy = averageApy,
        )
}