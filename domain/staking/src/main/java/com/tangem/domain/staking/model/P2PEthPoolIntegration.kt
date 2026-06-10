package com.tangem.domain.staking.model

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.staking.YieldToken
import com.tangem.domain.staking.model.common.RewardClaiming
import com.tangem.domain.staking.model.common.RewardSchedule
import com.tangem.domain.staking.model.common.StakingActionArgs
import com.tangem.domain.staking.model.common.StakingAmountRequirement
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import com.tangem.domain.staking.model.ethpool.VaultLimitInfo
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * StakingIntegration implementation for P2PEthPool pooled staking.
 * Converts P2PEthPoolVault data to the common StakingIntegration interface.
 */
class P2PEthPoolIntegration(
    override val integrationId: StakingIntegrationID,
    private val vaults: List<P2PEthPoolVault>,
    private val vaultLimits: Map<String, VaultLimitInfo>,
) : StakingIntegration {

    // Basic

    override val token: YieldToken = YieldToken.ETH

    override val tokens: List<YieldToken> = listOf(token)

    // Targets (vaults)

    override val targets: List<StakingTarget> = vaults.map { vault ->
        vault.toStakingTarget()
    }

    override val preferredTargets: List<StakingTarget> = vaults
        .filter { isVaultAvailable(it) }
        .map { it.toStakingTarget() }

    override val areAllTargetsFull: Boolean = preferredTargets.isEmpty()

    // Enter/Exit Args

    override val isPartialAmountDisabled: Boolean = false

    override val enterMinimumAmount: BigDecimal = DEFAULT_MINIMUM_STAKE

    override val exitMinimumAmount: BigDecimal = DEFAULT_MINIMUM_UNSTAKE

    override val enterArgs: StakingActionArgs = StakingActionArgs(
        amountRequirement = StakingAmountRequirement(
            isRequired = true,
            minimum = enterMinimumAmount,
            maximum = calculateMaximumStakeAmount(),
        ),
        isPartialAmountDisabled = false,
    )

    override val exitArgs: StakingActionArgs = StakingActionArgs(
        amountRequirement = StakingAmountRequirement(
            isRequired = true,
            minimum = exitMinimumAmount,
            maximum = null,
        ),
        isPartialAmountDisabled = false,
    )

    // Metadata

    override val warmupPeriod: Period = Period.Days(0)

    override val cooldownPeriod: CooldownPeriod = CooldownPeriod.Range(
        minDays = MIN_COOLDOWN_DAYS,
        maxDays = MAX_COOLDOWN_DAYS,
    )

    override val rewardSchedule: RewardSchedule = RewardSchedule.DAY

    override val rewardClaiming: RewardClaiming = RewardClaiming.AUTO

    // Legal URLs

    override val legalUrls: StakingLegalUrls = StakingLegalUrls(
        termsOfServiceUrl = TERMS_OF_SERVICE_URL,
        privacyPolicyUrl = PRIVACY_POLICY_URL,
    )

    override fun getCurrentToken(rawCurrencyId: CryptoCurrency.RawID?): YieldToken = token

    private fun isVaultAvailable(vault: P2PEthPoolVault): Boolean {
        val info = vaultLimits[vault.vaultAddress.lowercase()] ?: return false
        return info.limit - vault.totalAssets > AVAILABILITY_THRESHOLD
    }

    private fun calculateMaximumStakeAmount(): BigDecimal? {
        return vaults
            .filter { isVaultAvailable(it) }
            .mapNotNull { vault ->
                vaultLimits[vault.vaultAddress.lowercase()]?.let { it.limit - vault.totalAssets }
            }
            .minOrNull()
            ?.setScale(MAX_AMOUNT_SCALE, RoundingMode.FLOOR)
    }

    companion object {
        private const val MIN_COOLDOWN_DAYS = 1
        private const val MAX_COOLDOWN_DAYS = 4
        private const val MAX_AMOUNT_SCALE = 1
        private val DEFAULT_MINIMUM_STAKE = BigDecimal("0.01")
        private val DEFAULT_MINIMUM_UNSTAKE = BigDecimal("0.01")
        private val AVAILABILITY_THRESHOLD = BigDecimal("0.1")

        private const val TERMS_OF_SERVICE_URL = "https://www.p2p.org/terms-of-use"
        private const val PRIVACY_POLICY_URL = "https://www.p2p.org/privacy-policy"
    }
}