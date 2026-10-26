package com.tangem.domain.staking.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.staking.model.ethpool.VaultLimitInfo
import com.tangem.test.mock.staking.MockStaking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class P2PEthPoolIntegrationTest {

    private fun buildVault(address: String, capacity: String, totalAssets: String) = MockStaking.vault(
        vaultAddress = address,
        capacity = BigDecimal(capacity),
        totalAssets = BigDecimal(totalAssets),
    )

    private fun buildLimits(vararg pairs: Pair<String, BigDecimal>) =
        pairs.associate { (addr, limit) ->
            addr.lowercase() to VaultLimitInfo(limit = limit, coefficient = BigDecimal("1.25"))
        }

    private fun emptyIntegration() = P2PEthPoolIntegration(StakingIntegrationID.P2PEthPool, emptyList(), emptyMap())

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class MaximumAmount {

        @Test
        fun `GIVEN vault with limit 50 and assets 10 WHEN enterArgs read THEN maximum is 40_0`() {
            // Arrange
            val vaults = listOf(buildVault("0xABC", capacity = "100", totalAssets = "10"))
            val limits = buildLimits("0xABC" to BigDecimal("50"))

            // Act
            val integration = P2PEthPoolIntegration(StakingIntegrationID.P2PEthPool, vaults, limits)

            // Assert
            assertThat(integration.enterArgs.amountRequirement!!.maximum).isEqualTo(BigDecimal("40.0"))
        }

        @Test
        fun `GIVEN fractional remaining capacity WHEN enterArgs read THEN maximum floored to one decimal`() {
            // Arrange
            val vaults = listOf(buildVault("0xABC", capacity = "100", totalAssets = "10"))
            val limits = buildLimits("0xABC" to BigDecimal("22.37"))

            // Act
            val integration = P2PEthPoolIntegration(StakingIntegrationID.P2PEthPool, vaults, limits)

            // Assert
            assertThat(integration.enterArgs.amountRequirement!!.maximum).isEqualTo(BigDecimal("12.3"))
        }

        @Test
        fun `GIVEN vault absent from limits WHEN enterArgs read THEN all targets full and maximum is null`() {
            // Arrange
            val vaults = listOf(buildVault("0xABC", capacity = "100", totalAssets = "30"))
            val limits = emptyMap<String, VaultLimitInfo>()

            // Act
            val integration = P2PEthPoolIntegration(StakingIntegrationID.P2PEthPool, vaults, limits)

            // Assert
            assertThat(integration.areAllTargetsFull).isTrue()
            assertThat(integration.enterArgs.amountRequirement!!.maximum).isNull()
        }

        @Test
        fun `GIVEN two available vaults WHEN enterArgs read THEN maximum is the smallest remaining capacity`() {
            // Arrange
            val vault1 = buildVault("0xA", capacity = "100", totalAssets = "10")
            val vault2 = buildVault("0xB", capacity = "100", totalAssets = "20")
            val limits = buildLimits("0xa" to BigDecimal("50"), "0xb" to BigDecimal("50"))

            // Act
            val integration = P2PEthPoolIntegration(StakingIntegrationID.P2PEthPool, listOf(vault1, vault2), limits)

            // Assert
            assertThat(integration.enterArgs.amountRequirement!!.maximum).isEqualTo(BigDecimal("30.0"))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Availability {

        @Test
        fun `GIVEN vault absent from limits WHEN areAllTargetsFull read THEN true`() {
            // Arrange
            val vaults = listOf(buildVault("0xABC", capacity = "100", totalAssets = "48"))
            val limits = emptyMap<String, VaultLimitInfo>()

            // Act
            val integration = P2PEthPoolIntegration(StakingIntegrationID.P2PEthPool, vaults, limits)

            // Assert
            assertThat(integration.areAllTargetsFull).isTrue()
        }

        @Test
        fun `GIVEN exactly 0_1 ETH remaining WHEN areAllTargetsFull read THEN true`() {
            // Arrange
            val vaults = listOf(buildVault("0xABC", capacity = "100", totalAssets = "49.9"))
            val limits = buildLimits("0xABC" to BigDecimal("50"))

            // Act
            val integration = P2PEthPoolIntegration(StakingIntegrationID.P2PEthPool, vaults, limits)

            // Assert
            assertThat(integration.areAllTargetsFull).isTrue()
        }

        @Test
        fun `GIVEN remaining just above 0_1 ETH WHEN areAllTargetsFull read THEN false`() {
            // Arrange
            val vaults = listOf(buildVault("0xABC", capacity = "400", totalAssets = "321.895202388313423922"))
            val limits = buildLimits("0xABC" to BigDecimal("322.1"))

            // Act
            val integration = P2PEthPoolIntegration(StakingIntegrationID.P2PEthPool, vaults, limits)

            // Assert
            assertThat(integration.areAllTargetsFull).isFalse()
        }

        @Test
        fun `GIVEN one full and one available vault WHEN areAllTargetsFull read THEN false`() {
            // Arrange
            val vault1 = buildVault("0xA", capacity = "100", totalAssets = "49.95")
            val vault2 = buildVault("0xB", capacity = "100", totalAssets = "10")
            val limits = buildLimits("0xa" to BigDecimal("50"), "0xb" to BigDecimal("50"))

            // Act
            val integration = P2PEthPoolIntegration(StakingIntegrationID.P2PEthPool, listOf(vault1, vault2), limits)

            // Assert
            assertThat(integration.areAllTargetsFull).isFalse()
        }

        @Test
        fun `GIVEN one full and one available vault WHEN preferredTargets read THEN only available vault returned`() {
            // Arrange
            val vault1 = buildVault("0xA", capacity = "100", totalAssets = "49.95")
            val vault2 = buildVault("0xB", capacity = "100", totalAssets = "10")
            val limits = buildLimits("0xa" to BigDecimal("50"), "0xb" to BigDecimal("50"))

            // Act
            val integration = P2PEthPoolIntegration(StakingIntegrationID.P2PEthPool, listOf(vault1, vault2), limits)

            // Assert
            assertThat(integration.preferredTargets).hasSize(1)
            assertThat(integration.preferredTargets.first().address).isEqualTo("0xB")
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class MinimumAmount {

        @Test
        fun `GIVEN no vaults WHEN enterMinimumAmount read THEN 0_01`() {
            // Arrange
            val integration = emptyIntegration()

            // Act & Assert
            assertThat(integration.enterMinimumAmount).isEqualTo(BigDecimal("0.01"))
        }

        @Test
        fun `GIVEN no vaults WHEN exitMinimumAmount read THEN 0_01`() {
            // Arrange
            val integration = emptyIntegration()

            // Act & Assert
            assertThat(integration.exitMinimumAmount).isEqualTo(BigDecimal("0.01"))
        }

        @Test
        fun `GIVEN no vaults WHEN exitArgs read THEN required minimum 0_01 without maximum`() {
            // Arrange
            val integration = emptyIntegration()

            // Act
            val exitRequirement = integration.exitArgs.amountRequirement!!

            // Assert
            assertThat(exitRequirement.isRequired).isTrue()
            assertThat(exitRequirement.minimum).isEqualTo(BigDecimal("0.01"))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class LimitsLookup {

        @Test
        fun `GIVEN mixed case vault address and lowercase limit key WHEN integration created THEN vault available`() {
            // Arrange
            val vaults = listOf(buildVault("0xABC", capacity = "100", totalAssets = "10"))
            val limits = mapOf("0xabc" to VaultLimitInfo(BigDecimal("50"), null))

            // Act
            val integration = P2PEthPoolIntegration(StakingIntegrationID.P2PEthPool, vaults, limits)

            // Assert
            assertThat(integration.areAllTargetsFull).isFalse()
            assertThat(integration.preferredTargets.map { it.address }).isEqualTo(listOf("0xABC"))
        }

        // Pins current behaviour: limits are looked up by vaultAddress.lowercase(), keys must be lowercase
        @Test
        fun `GIVEN mixed case limit key WHEN integration created THEN vault treated as full`() {
            // Arrange
            val vaults = listOf(buildVault("0xABC", capacity = "100", totalAssets = "10"))
            val limits = mapOf("0xABC" to VaultLimitInfo(BigDecimal("50"), null))

            // Act
            val integration = P2PEthPoolIntegration(StakingIntegrationID.P2PEthPool, vaults, limits)

            // Assert
            assertThat(integration.areAllTargetsFull).isTrue()
            assertThat(integration.preferredTargets).isEmpty()
            assertThat(integration.enterArgs.amountRequirement!!.maximum).isNull()
        }

        @Test
        fun `GIVEN negative remaining capacity WHEN integration created THEN vault full and maximum null`() {
            // Arrange
            val vaults = listOf(buildVault("0xabc", capacity = "100", totalAssets = "20"))
            val limits = buildLimits("0xabc" to BigDecimal("10"))

            // Act
            val integration = P2PEthPoolIntegration(StakingIntegrationID.P2PEthPool, vaults, limits)

            // Assert
            assertThat(integration.areAllTargetsFull).isTrue()
            assertThat(integration.enterArgs.amountRequirement!!.maximum).isNull()
        }

        @Test
        fun `GIVEN full and available vaults WHEN targets read THEN all vaults returned`() {
            // Arrange
            val vaultA = buildVault("0xa", capacity = "100", totalAssets = "49.95")
            val vaultB = buildVault("0xb", capacity = "100", totalAssets = "10")
            val limits = buildLimits("0xa" to BigDecimal("50"), "0xb" to BigDecimal("50"))

            // Act
            val integration = P2PEthPoolIntegration(StakingIntegrationID.P2PEthPool, listOf(vaultA, vaultB), limits)

            // Assert
            assertThat(integration.targets).isEqualTo(
                listOf(StakingTarget.Vault(vaultA), StakingTarget.Vault(vaultB)),
            )
            assertThat(integration.allTargets).isEqualTo(integration.targets)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class EmptyVaults {

        @Test
        fun `GIVEN no vaults WHEN areAllTargetsFull read THEN true`() {
            // Arrange
            val integration = emptyIntegration()

            // Act & Assert
            assertThat(integration.areAllTargetsFull).isTrue()
        }
    }
}