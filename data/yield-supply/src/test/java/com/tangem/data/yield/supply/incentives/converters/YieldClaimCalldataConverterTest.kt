package com.tangem.data.yield.supply.incentives.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.yield.supply.models.incentives.YieldClaimCalldata
import com.tangem.grow.datasource.yield.models.incentives.YieldClaimCalldataResponse
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class YieldClaimCalldataConverterTest {

    @Test
    fun `GIVEN claimable payload WHEN convert THEN maps every field`() {
        // Arrange
        val response = YieldClaimCalldataResponse(
            chainId = 1,
            userAddress = "0xUser",
            moduleAddress = "0xModule",
            hasClaimable = true,
            to = "0xModule",
            data = "0x1a2b3c",
            rewardTokens = listOf("0xReward"),
            cumulativeAmounts = listOf("123000000"),
            totalEstimatedServiceFeeUsd = BigDecimal("18.45"),
            isClaimRecommended = true,
        )

        // Act
        val actual = YieldClaimCalldataConverter.convert(response)

        // Assert
        assertThat(actual).isEqualTo(
            YieldClaimCalldata(
                chainId = 1,
                userAddress = "0xUser",
                moduleAddress = "0xModule",
                hasClaimable = true,
                to = "0xModule",
                data = "0x1a2b3c",
                rewardTokens = listOf("0xReward"),
                cumulativeAmounts = listOf("123000000"),
                totalEstimatedServiceFeeUsd = BigDecimal("18.45"),
                isClaimRecommended = true,
            ),
        )
    }

    @Test
    fun `GIVEN nothing to claim WHEN convert THEN transaction fields stay null`() {
        // Arrange
        val response = YieldClaimCalldataResponse(
            chainId = 1,
            userAddress = "0xUser",
            moduleAddress = null,
            hasClaimable = false,
            to = null,
            data = null,
        )

        // Act
        val actual = YieldClaimCalldataConverter.convert(response)

        // Assert
        assertThat(actual).isEqualTo(
            YieldClaimCalldata(
                chainId = 1,
                userAddress = "0xUser",
                moduleAddress = null,
                hasClaimable = false,
                to = null,
                data = null,
                rewardTokens = emptyList(),
                cumulativeAmounts = emptyList(),
                totalEstimatedServiceFeeUsd = BigDecimal.ZERO,
                isClaimRecommended = false,
            ),
        )
    }
}