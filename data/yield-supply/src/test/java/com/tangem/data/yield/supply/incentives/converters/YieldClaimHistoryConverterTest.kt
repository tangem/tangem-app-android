package com.tangem.data.yield.supply.incentives.converters

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.yield.supply.models.incentives.YieldClaimHistoryItem
import com.tangem.grow.datasource.yield.models.incentives.YieldClaimHistoryEntryDto
import com.tangem.grow.datasource.yield.models.incentives.YieldClaimHistoryResponse
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class YieldClaimHistoryConverterTest {

    @Test
    fun `GIVEN claim entry WHEN convert THEN maps every field`() {
        // Arrange
        val response = YieldClaimHistoryResponse(
            chainId = 42161,
            claims = listOf(
                YieldClaimHistoryEntryDto(
                    tokenAddress = "0xReward",
                    amount = "12450000000000000000",
                    blockNumber = 351829471,
                    timestamp = 1753488600,
                    campaignId = "0xabc123",
                    root = "0xdef456",
                    isLatest = true,
                ),
            ),
        )

        // Act
        val actual = YieldClaimHistoryConverter.convert(response)

        // Assert
        assertThat(actual).containsExactly(
            YieldClaimHistoryItem(
                tokenAddress = "0xReward",
                amount = BigInteger("12450000000000000000"),
                timestamp = Instant.fromEpochSeconds(1753488600),
                blockNumber = 351829471,
                campaignId = "0xabc123",
                isLatest = true,
            ),
        )
    }

    @Test
    fun `GIVEN no claims WHEN convert THEN list is empty`() {
        // Arrange
        val response = YieldClaimHistoryResponse(chainId = 1, claims = null)

        // Act
        val actual = YieldClaimHistoryConverter.convert(response)

        // Assert
        assertThat(actual).isEmpty()
    }
}