package com.tangem.data.yield.supply.incentives.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.yield.supply.models.incentives.YieldAprHistory
import com.tangem.grow.datasource.yield.models.incentives.YieldAprHistoryPointDto
import com.tangem.grow.datasource.yield.models.incentives.YieldAprHistoryResponse
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class YieldAprHistoryConverterTest {

    @Test
    fun `GIVEN history point WHEN convert THEN maps every field`() {
        // Arrange
        val response = YieldAprHistoryResponse(
            chainId = 1,
            tokenAddress = "0xToken",
            points = listOf(
                YieldAprHistoryPointDto(
                    date = "2026-07-31",
                    baseApy = BigDecimal("3.2"),
                    rewardsApr = BigDecimal("1.19"),
                    totalApyEst = BigDecimal("4.39"),
                    isCarriedForward = true,
                ),
            ),
        )

        // Act
        val actual = YieldAprHistoryConverter.convert(response)

        // Assert
        assertThat(actual).isEqualTo(
            YieldAprHistory(
                chainId = 1,
                tokenAddress = "0xToken",
                points = listOf(
                    YieldAprHistory.Point(
                        date = LocalDate(year = 2026, monthNumber = 7, dayOfMonth = 31),
                        baseApy = BigDecimal("3.2"),
                        rewardsApr = BigDecimal("1.19"),
                        totalApyEst = BigDecimal("4.39"),
                        isCarriedForward = true,
                    ),
                ),
            ),
        )
    }

    @Test
    fun `GIVEN unparseable date WHEN convert THEN it throws for the repository to contain`() {
        // Arrange
        val response = YieldAprHistoryResponse(
            chainId = 1,
            tokenAddress = "0xToken",
            points = listOf(YieldAprHistoryPointDto(date = "31 July")),
        )

        // Act
        val actual = runCatching { YieldAprHistoryConverter.convert(response) }.exceptionOrNull()

        // Assert
        assertThat(actual).isInstanceOf(IllegalArgumentException::class.java)
    }
}