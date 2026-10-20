package com.tangem.data.staking.converters.ethpool

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.staking.P2PEthPoolExitRequest
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolExitRequestDTO
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

internal class P2PEthPoolStakingAccountConverterTest {

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