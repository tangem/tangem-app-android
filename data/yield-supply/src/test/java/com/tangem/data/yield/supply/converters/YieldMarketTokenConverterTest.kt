package com.tangem.data.yield.supply.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.grow.datasource.yield.models.YieldSupplyMarketTokenDto
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.domain.yield.supply.models.incentives.YieldIncentiveCampaign
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

    @Test
    fun `GIVEN incentive overlay WHEN convert THEN campaign is kept`() {
        // Arrange
        val dto = YieldSupplyMarketTokenDto(
            tokenAddress = "0xToken",
            apy = BigDecimal("4.12"),
            isActive = true,
            chainId = 1,
            maxFeeNative = BigDecimal("0.005"),
            maxFeeUSD = BigDecimal("12.34"),
            hasActiveCampaign = true,
            rewardsApr = BigDecimal("2.08"),
            totalApyEst = BigDecimal("6.2"),
        )

        // Act
        val result = YieldMarketTokenConverter.convert(dto)

        // Assert
        assertThat(result.campaign).isEqualTo(
            YieldIncentiveCampaign(
                hasActiveCampaign = true,
                rewardsApr = BigDecimal("2.08"),
                totalApyEst = BigDecimal("6.2"),
            ),
        )
    }

    @Test
    fun `GIVEN campaign flag without apr values WHEN convert THEN overlay falls back to zero`() {
        // Arrange
        val dto = YieldSupplyMarketTokenDto(hasActiveCampaign = false)

        // Act
        val result = YieldMarketTokenConverter.convert(dto)

        // Assert
        assertThat(result.campaign).isEqualTo(
            YieldIncentiveCampaign(
                hasActiveCampaign = false,
                rewardsApr = BigDecimal.ZERO,
                totalApyEst = BigDecimal.ZERO,
            ),
        )
    }
}