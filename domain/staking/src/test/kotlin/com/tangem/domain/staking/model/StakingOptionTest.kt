package com.tangem.domain.staking.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.staking.YieldToken
import com.tangem.test.mock.staking.MockStaking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

internal class StakingOptionTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class P2PEthPool {

        @Test
        fun `GIVEN two vaults WHEN P2PEthPool option created THEN max apy and ETH token`() {
            // Arrange
            val vaults = listOf(
                MockStaking.vault(apy = BigDecimal("0.03")),
                MockStaking.vault(vaultAddress = "0xvault2", apy = BigDecimal("0.045")),
            )

            // Act
            val actual = StakingOption.P2PEthPool(vaults)

            // Assert
            assertThat(actual.apy).isEqualTo(BigDecimal("0.045"))
            assertThat(actual.token).isEqualTo(YieldToken.ETH)
            assertThat(actual.isAvailable).isTrue()
            assertThat(actual.integrationId).isEqualTo(StakingIntegrationID.P2PEthPool)
        }

        // Pins current behaviour: maxOf on an empty vault list throws before isAvailable can report false
        @Test
        fun `GIVEN no vaults WHEN P2PEthPool option created THEN throws NoSuchElementException`() {
            // Act
            val error = runCatching { StakingOption.P2PEthPool(emptyList()) }.exceptionOrNull()

            // Assert
            assertThat(error).isInstanceOf(NoSuchElementException::class.java)
        }
    }
}