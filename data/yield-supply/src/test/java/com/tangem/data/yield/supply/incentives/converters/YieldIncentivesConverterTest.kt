package com.tangem.data.yield.supply.incentives.converters

import arrow.core.left
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.yield.supply.models.incentives.YieldIncentivesError
import com.tangem.domain.yield.supply.models.incentives.YieldReward
import com.tangem.domain.yield.supply.models.incentives.YieldRewardDestination
import com.tangem.grow.datasource.yield.models.incentives.YieldIncentivesResponse
import com.tangem.grow.datasource.yield.models.incentives.YieldRewardDto
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

internal class YieldIncentivesConverterTest {

    @Test
    fun `GIVEN unknown destination WHEN convert THEN reward is unsupported`() {
        // Arrange
        val dto = createRewardDto(finalDestination = "something-new")

        // Act
        val actual = YieldIncentivesConverter.convertReward(dto)

        // Assert
        assertThat(actual.finalDestination).isEqualTo(YieldRewardDestination.Unsupported)
        assertThat(actual.claimableAmount).isEqualTo(BigInteger("1500000"))
    }

    @Test
    fun `GIVEN fully populated reward WHEN convert THEN maps every field`() {
        // Arrange
        val dto = createRewardDto(finalDestination = "owner")

        // Act
        val actual = YieldIncentivesConverter.convertReward(dto)

        // Assert
        assertThat(actual).isEqualTo(
            YieldReward(
                rewardToken = "0xAbC",
                symbol = "USDC",
                tokenDecimals = 6,
                claimableAmount = BigInteger("1500000"),
                pendingAmount = BigInteger("250000"),
                usdValue = BigDecimal("1.50"),
                estimatedNetValueUsd = BigDecimal("1.42"),
                estimatedServiceFeeUsd = BigDecimal("0.08"),
                serviceFeeRate = BigDecimal("0.05"),
                finalDestination = YieldRewardDestination.Owner,
            ),
        )
    }

    @Test
    fun `GIVEN destination in mixed case WHEN convert THEN matched case-insensitively`() {
        // Arrange
        val dto = createRewardDto(finalDestination = "AAVE")

        // Act
        val actual = YieldIncentivesConverter.convertReward(dto)

        // Assert
        assertThat(actual.finalDestination).isEqualTo(YieldRewardDestination.Aave)
    }

    @Test
    fun `GIVEN missing amounts WHEN convert THEN amounts default to zero`() {
        // Arrange
        val dto = createRewardDto(finalDestination = "module").copy(
            claimableAmount = null,
            pendingAmount = "not-a-number",
            usdValue = null,
            estimatedNetValueUsd = null,
            estimatedServiceFeeUsd = null,
            serviceFeeRateBps = null,
            tokenDecimals = null,
        )

        // Act
        val actual = YieldIncentivesConverter.convertReward(dto)

        // Assert
        assertThat(actual).isEqualTo(
            YieldReward(
                rewardToken = "0xAbC",
                symbol = "USDC",
                tokenDecimals = 0,
                claimableAmount = BigInteger.ZERO,
                pendingAmount = BigInteger.ZERO,
                usdValue = BigDecimal.ZERO,
                estimatedNetValueUsd = BigDecimal.ZERO,
                estimatedServiceFeeUsd = BigDecimal.ZERO,
                serviceFeeRate = BigDecimal.ZERO,
                finalDestination = YieldRewardDestination.Module,
            ),
        )
    }

    @Test
    fun `GIVEN incentives response WHEN convert THEN module fields are mapped`() {
        // Arrange
        val response = createIncentivesResponse(isClaimRecommended = false)

        // Act
        val actual = YieldIncentivesConverter.convert(response)

        // Assert
        val incentives = actual.getOrNull()
        assertThat(incentives?.chainId).isEqualTo(1)
        assertThat(incentives?.userAddress).isEqualTo("0xUser")
        assertThat(incentives?.moduleAddress).isNull()
        assertThat(incentives?.rewards).hasSize(1)
        assertThat(incentives?.lastUpdatedAt).isEqualTo(Instant.parse("2026-07-31T10:30:00.000Z"))
    }

    @Test
    fun `GIVEN claimRecommended on the response WHEN convert THEN it lands on the model`() {
        // Arrange
        val response = createIncentivesResponse(isClaimRecommended = true)

        // Act
        val actual = YieldIncentivesConverter.convert(response)

        // Assert
        assertThat(actual.getOrNull()?.isClaimRecommended).isTrue()
    }

    @Test
    fun `GIVEN response without claimRecommended WHEN convert THEN the payload is refused`() {
        // Arrange — a missing key must not read as "not recommended": that is indistinguishable from the
        // below-threshold state and would take the Claim action dark without saying why.
        val response = createIncentivesResponse(isClaimRecommended = null)

        // Act
        val actual = YieldIncentivesConverter.convert(response)

        // Assert
        assertThat(actual).isEqualTo(YieldIncentivesError.Unavailable.left())
    }

    @Test
    fun `GIVEN an unparsable timestamp WHEN convert THEN the rest of the payload still maps`() {
        // Arrange
        val response = createIncentivesResponse(isClaimRecommended = true).copy(lastUpdatedAt = "not a timestamp")

        // Act
        val actual = YieldIncentivesConverter.convert(response)

        // Assert
        assertThat(actual.isRight()).isTrue()
        assertThat(actual.getOrNull()?.lastUpdatedAt).isNull()
    }

    private fun createIncentivesResponse(isClaimRecommended: Boolean?) = YieldIncentivesResponse(
        chainId = 1,
        userAddress = "0xUser",
        moduleAddress = null,
        isClaimRecommended = isClaimRecommended,
        rewards = listOf(createRewardDto(finalDestination = "owner")),
        lastUpdatedAt = "2026-07-31T10:30:00.000Z",
    )

    private fun createRewardDto(finalDestination: String?) = YieldRewardDto(
        source = "merkl",
        protocol = "aave",
        rewardToken = "0xAbC",
        symbol = "USDC",
        tokenDecimals = 6,
        claimableAmount = "1500000",
        pendingAmount = "250000",
        usdValue = BigDecimal("1.50"),
        estimatedNetValueUsd = BigDecimal("1.42"),
        estimatedServiceFeeUsd = BigDecimal("0.08"),
        serviceFeeRateBps = 500,
        finalDestination = finalDestination,
    )
}