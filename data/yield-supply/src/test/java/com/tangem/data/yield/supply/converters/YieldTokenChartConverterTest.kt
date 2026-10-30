package com.tangem.data.yield.supply.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.grow.datasource.yield.models.YieldTokenChartResponse
import com.tangem.domain.yield.supply.models.YieldSupplyMarketChartData
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlinx.datetime.LocalDate

internal class YieldTokenChartConverterTest {

    @Test
    fun `GIVEN response with data points WHEN convert THEN splits avgApy into y and bucketIndex into x preserving order`() {
        // Arrange
        val response = response(
            averageApy = BigDecimal("4.25"),
            points = listOf(
                point(bucketIndex = 0, avgApy = "3.5", start = "2025-10-30T00:00:00.000Z"),
                point(bucketIndex = 1, avgApy = "4.0", start = "2025-11-11T00:00:00.000Z"),
                point(bucketIndex = 2, avgApy = "5.0", start = "2025-11-23T00:00:00.000Z"),
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
                bucketStarts = listOf(
                    LocalDate(year = 2025, monthNumber = 10, dayOfMonth = 30),
                    LocalDate(year = 2025, monthNumber = 11, dayOfMonth = 11),
                    LocalDate(year = 2025, monthNumber = 11, dayOfMonth = 23),
                ),
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

    @Test
    fun `GIVEN a bucket without a readable start WHEN convert THEN keeps the values and drops every bucket date`() {
        // Arrange
        val response = response(
            averageApy = BigDecimal("4.25"),
            points = listOf(
                point(bucketIndex = 0, avgApy = "3.5", start = "2025-10-30T00:00:00.000Z"),
                point(bucketIndex = 1, avgApy = "4.0", start = null),
                point(bucketIndex = 2, avgApy = "5.0", start = "not a date"),
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
                bucketStarts = emptyList(),
            ),
        )
    }

    private fun point(bucketIndex: Int, avgApy: String, start: String?) = YieldTokenChartResponse.DataPoint(
        bucketIndex = bucketIndex,
        avgApy = BigDecimal(avgApy),
        start = start,
    )

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