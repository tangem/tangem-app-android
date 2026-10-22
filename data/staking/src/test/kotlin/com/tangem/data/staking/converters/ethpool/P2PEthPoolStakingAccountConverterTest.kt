package com.tangem.data.staking.converters.ethpool

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.staking.P2PEthPoolExitQueue
import com.tangem.domain.models.staking.P2PEthPoolExitRequest
import com.tangem.domain.models.staking.P2PEthPoolStake
import com.tangem.domain.models.staking.P2PEthPoolStakingAccount
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolAccountResponse
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolExitQueueDTO
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolExitRequestDTO
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolStakeDTO
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

internal class P2PEthPoolStakingAccountConverterTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Convert {

        @Test
        fun `GIVEN account with distinct balances and exit queue WHEN convert THEN every field mapped`() {
            // Arrange
            val response = P2PEthPoolAccountResponse(
                delegatorAddress = "0xdelegator",
                vaultAddress = "0xvault",
                stake = P2PEthPoolStakeDTO(assets = BigDecimal("3.1"), totalEarnedAssets = BigDecimal("0.07")),
                availableToUnstake = BigDecimal("2.9"),
                availableToWithdraw = BigDecimal("0.6"),
                exitQueue = P2PEthPoolExitQueueDTO(
                    total = BigDecimal("0.4"),
                    requests = listOf(exitRequestDTO(withdrawalTimestamp = 1_700_086_400_000L, isClaimable = true)),
                ),
            )

            // Act
            val actual = P2PEthPoolStakingAccountConverter.convert(response)

            // Assert
            val expected = P2PEthPoolStakingAccount(
                delegatorAddress = "0xdelegator",
                vaultAddress = "0xvault",
                stake = P2PEthPoolStake(assets = BigDecimal("3.1"), totalEarnedAssets = BigDecimal("0.07")),
                availableToUnstake = BigDecimal("2.9"),
                availableToWithdraw = BigDecimal("0.6"),
                exitQueue = P2PEthPoolExitQueue(
                    total = BigDecimal("0.4"),
                    requests = listOf(
                        P2PEthPoolExitRequest(
                            ticket = "t1",
                            totalAssets = BigDecimal("0.4"),
                            timestamp = Instant.fromEpochMilliseconds(1_700_000_000_000L),
                            withdrawalTimestamp = Instant.fromEpochMilliseconds(1_700_086_400_000L),
                            isClaimable = true,
                        ),
                    ),
                ),
            )
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ConvertExitRequest {

        @Test
        fun `GIVEN exit request with withdrawal time WHEN convertExitRequest THEN millis become instants`() {
            // Arrange
            val dto = exitRequestDTO(withdrawalTimestamp = 1_700_086_400_000L, isClaimable = true)

            // Act
            val actual = P2PEthPoolStakingAccountConverter.convertExitRequest(dto)

            // Assert
            val expected = P2PEthPoolExitRequest(
                ticket = "t1",
                totalAssets = BigDecimal("0.4"),
                timestamp = Instant.fromEpochMilliseconds(1_700_000_000_000L),
                withdrawalTimestamp = Instant.fromEpochMilliseconds(1_700_086_400_000L),
                isClaimable = true,
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `GIVEN exit request without withdrawal time WHEN convertExitRequest THEN withdrawal timestamp null`() {
            // Act
            val actual = P2PEthPoolStakingAccountConverter.convertExitRequest(exitRequestDTO())

            // Assert
            assertThat(actual.withdrawalTimestamp).isNull()
        }
    }

    private fun exitRequestDTO(
        ticket: String = "t1",
        totalAssets: BigDecimal = BigDecimal("0.4"),
        timestamp: Long = 1_700_000_000_000L,
        withdrawalTimestamp: Long? = null,
        isClaimable: Boolean = false,
    ) = P2PEthPoolExitRequestDTO(
        ticket = ticket,
        totalAssets = totalAssets,
        timestamp = timestamp,
        withdrawalTimestamp = withdrawalTimestamp,
        isClaimable = isClaimable,
    )
}