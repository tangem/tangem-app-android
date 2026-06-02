package com.tangem.domain.staking.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import com.tangem.domain.staking.model.ethpool.VaultLimitInfo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class P2PEthPoolIntegrationTest {

    private fun buildVault(
        address: String,
        capacity: String,
        totalAssets: String,
    ) = P2PEthPoolVault(
        vaultAddress = address,
        displayName = "Test Vault",
        apy = BigDecimal("4.5"),
        baseApy = BigDecimal("4.0"),
        capacity = BigDecimal(capacity),
        totalAssets = BigDecimal(totalAssets),
        feePercent = BigDecimal("0.1"),
        isPrivate = false,
        isGenesis = false,
        isSmoothingPool = true,
        isErc20 = false,
        tokenName = null,
        tokenSymbol = null,
        createdAt = 0L,
    )

    private fun buildLimits(vararg pairs: Pair<String, BigDecimal>) =
        pairs.associate { (addr, limit) ->
            addr.lowercase() to VaultLimitInfo(limit = limit, coefficient = BigDecimal("1.25"))
        }

    @Nested
    inner class MaximumAmount {
        @Test
        fun `vault available - uses remaining space as max, rounded down to 0_1 ETH`() {
            val vaults = listOf(buildVault("0xABC", capacity = "100", totalAssets = "10"))
            val limits = buildLimits("0xABC" to BigDecimal("50"))
            val integration = P2PEthPoolIntegration(StakingIntegrationID.P2PEthPool, vaults, limits)

            assertThat(integration.enterArgs!!.amountRequirement!!.maximum).isEqualTo(BigDecimal("40.0"))
        }

        @Test
        fun `remaining with fractional ETH - floored to 0_1 ETH precision`() {
            val vaults = listOf(buildVault("0xABC", capacity = "100", totalAssets = "10"))
            val limits = buildLimits("0xABC" to BigDecimal("22.37"))
            val integration = P2PEthPoolIntegration(StakingIntegrationID.P2PEthPool, vaults, limits)

            assertThat(integration.enterArgs!!.amountRequirement!!.maximum).isEqualTo(BigDecimal("12.3"))
        }

        @Test
        fun `vault absent from limits map - treated as full, max is null`() {
            val vaults = listOf(buildVault("0xABC", capacity = "100", totalAssets = "30"))
            val limits = emptyMap<String, VaultLimitInfo>()
            val integration = P2PEthPoolIntegration(StakingIntegrationID.P2PEthPool, vaults, limits)

            assertThat(integration.areAllTargetsFull).isTrue()
            assertThat(integration.enterArgs!!.amountRequirement!!.maximum).isNull()
        }

        @Test
        fun `vault with exactly 2 ETH remaining - not available, max is null`() {
            val vaults = listOf(buildVault("0xABC", capacity = "100", totalAssets = "48"))
            val limits = buildLimits("0xABC" to BigDecimal("50"))
            val integration = P2PEthPoolIntegration(StakingIntegrationID.P2PEthPool, vaults, limits)

            assertThat(integration.enterArgs!!.amountRequirement!!.maximum).isNull()
        }

        @Test
        fun `vault with less than 2 ETH remaining - not available, max is null`() {
            val vaults = listOf(buildVault("0xABC", capacity = "100", totalAssets = "48.5"))
            val limits = buildLimits("0xABC" to BigDecimal("50"))
            val integration = P2PEthPoolIntegration(StakingIntegrationID.P2PEthPool, vaults, limits)

            assertThat(integration.enterArgs!!.amountRequirement!!.maximum).isNull()
        }

        @Test
        fun `multiple available vaults - uses minimum remaining space`() {
            val vault1 = buildVault("0xA", capacity = "100", totalAssets = "10")
            val vault2 = buildVault("0xB", capacity = "100", totalAssets = "20")
            val limits = buildLimits("0xa" to BigDecimal("50"), "0xb" to BigDecimal("50"))
            val integration = P2PEthPoolIntegration(StakingIntegrationID.P2PEthPool, listOf(vault1, vault2), limits)

            assertThat(integration.enterArgs!!.amountRequirement!!.maximum).isEqualTo(BigDecimal("30.0"))
        }
    }

    @Nested
    inner class Availability {
        @Test
        fun `all vaults full - areAllTargetsFull is true`() {
            val vaults = listOf(buildVault("0xABC", capacity = "100", totalAssets = "48"))
            val limits = buildLimits("0xABC" to BigDecimal("50"))
            val integration = P2PEthPoolIntegration(StakingIntegrationID.P2PEthPool, vaults, limits)

            assertThat(integration.areAllTargetsFull).isTrue()
        }

        @Test
        fun `vault with remaining between 0_1 and 2 ETH - also considered full`() {
            val vaults = listOf(buildVault("0xABC", capacity = "100", totalAssets = "48.5"))
            val limits = buildLimits("0xABC" to BigDecimal("50"))
            val integration = P2PEthPoolIntegration(StakingIntegrationID.P2PEthPool, vaults, limits)

            assertThat(integration.areAllTargetsFull).isTrue()
        }

        @Test
        fun `at least one vault available - areAllTargetsFull is false`() {
            val vault1 = buildVault("0xA", capacity = "100", totalAssets = "48.5") // full (1.5 remaining < 2)
            val vault2 = buildVault("0xB", capacity = "100", totalAssets = "10") // available (40 remaining > 2)
            val limits = buildLimits("0xa" to BigDecimal("50"), "0xb" to BigDecimal("50"))
            val integration = P2PEthPoolIntegration(StakingIntegrationID.P2PEthPool, listOf(vault1, vault2), limits)

            assertThat(integration.areAllTargetsFull).isFalse()
        }

        @Test
        fun `preferred targets only contains available vaults`() {
            val vault1 = buildVault("0xA", capacity = "100", totalAssets = "48.5") // full
            val vault2 = buildVault("0xB", capacity = "100", totalAssets = "10") // available
            val limits = buildLimits("0xa" to BigDecimal("50"), "0xb" to BigDecimal("50"))
            val integration = P2PEthPoolIntegration(StakingIntegrationID.P2PEthPool, listOf(vault1, vault2), limits)

            assertThat(integration.preferredTargets).hasSize(1)
            assertThat(integration.preferredTargets.first().address).isEqualTo("0xB")
        }
    }

    @Nested
    inner class MinimumAmount {
        @Test
        fun `minimum stake is 0_01 ETH`() {
            val integration = P2PEthPoolIntegration(StakingIntegrationID.P2PEthPool, emptyList(), emptyMap())

            assertThat(integration.enterMinimumAmount).isEqualTo(BigDecimal("0.01"))
        }

        @Test
        fun `minimum unstake is 0_01 ETH`() {
            val integration = P2PEthPoolIntegration(StakingIntegrationID.P2PEthPool, emptyList(), emptyMap())

            assertThat(integration.exitMinimumAmount).isEqualTo(BigDecimal("0.01"))
        }

        @Test
        fun `exit args expose minimum unstake requirement of 0_01 ETH`() {
            val integration = P2PEthPoolIntegration(StakingIntegrationID.P2PEthPool, emptyList(), emptyMap())

            val exitRequirement = integration.exitArgs!!.amountRequirement!!
            assertThat(exitRequirement.isRequired).isTrue()
            assertThat(exitRequirement.minimum).isEqualTo(BigDecimal("0.01"))
        }
    }
}