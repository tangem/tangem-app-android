package com.tangem.data.yield.supply.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.tangemTech.models.YieldSupplyMarketTokenDto
import com.tangem.domain.yield.supply.models.YieldMarketToken
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class YieldMarketTokenConverterTest {

    @Test
    fun `GIVEN fully populated dto WHEN convert THEN maps every field`() {
        // Arrange
        val dto = YieldSupplyMarketTokenDto(
            tokenAddress = "0xToken",
            tokenSymbol = "USDT",
            tokenName = "Tether",
            apy = BigDecimal("5.5"),
            decimals = 6,
            isActive = true,
            chainId = 1,
            maxFeeNative = BigDecimal("0.005"),
            maxFeeUSD = BigDecimal("12.34"),
        )

        // Act
        val result = YieldMarketTokenConverter.convert(dto)

        // Assert
        assertThat(result).isEqualTo(
            YieldMarketToken(
                tokenAddress = "0xToken",
                chainId = 1,
                apy = BigDecimal("5.5"),
                isActive = true,
                maxFeeNative = BigDecimal("0.005"),
                maxFeeUSD = BigDecimal("12.34"),
                backendId = null,
            ),
        )
    }

    @Test
    fun `GIVEN dto with null fields WHEN convert THEN applies defaults`() {
        // Arrange
        val dto = YieldSupplyMarketTokenDto()

        // Act
        val result = YieldMarketTokenConverter.convert(dto)

        // Assert
        assertThat(result).isEqualTo(
            YieldMarketToken(
                tokenAddress = "",
                chainId = -1,
                apy = BigDecimal.ZERO,
                isActive = false,
                maxFeeNative = BigDecimal.ZERO,
                maxFeeUSD = BigDecimal.ZERO,
                backendId = null,
            ),
        )
    }
}