package com.tangem.domain.staking.model

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.staking.YieldToken
import com.tangem.domain.staking.model.common.RewardClaiming
import com.tangem.domain.staking.model.common.RewardSchedule
import com.tangem.domain.staking.model.common.StakingActionArgs
import com.tangem.domain.staking.model.common.StakingAmountRequirement
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import java.math.BigDecimal

/**
 * StakingIntegration implementation for P2PEthPool pooled staking.
 * Converts P2PEthPoolVault data to the common StakingIntegration interface.
 */
class P2PEthPoolIntegration(
    override val integrationId: StakingIntegrationID,
    private val vaults: List<P2PEthPoolVault>,
) : StakingIntegration {

    // Basic

    override val token: YieldToken = YieldToken.ETH

    override val tokens: List<YieldToken> = listOf(token)

    // Targets (vaults)

    override val targets: List<StakingTarget> = vaults.map { vault ->
        vault.toStakingTarget()
    }

    override val preferredTargets: List<StakingTarget> = targets

    override val areAllTargetsFull: Boolean = false

    // Enter/Exit Args

    override val isPartialAmountDisabled: Boolean = false

    override val enterMinimumAmount: BigDecimal = DEFAULT_MINIMUM_STAKE

    override val exitMinimumAmount: BigDecimal? = null

    override val enterArgs: StakingActionArgs = StakingActionArgs(
        amountRequirement = StakingAmountRequirement(
            isRequired = true,
            minimum = DEFAULT_MINIMUM_STAKE,
            maximum = calculateMaximumStakeAmount(),
        ),
        isPartialAmountDisabled = false,
    )

    override val exitArgs: StakingActionArgs = StakingActionArgs(
        amountRequirement = StakingAmountRequirement(
            isRequired = true,
            minimum = null,
            maximum = null,
        ),
        isPartialAmountDisabled = false,
    )

    // Metadata

    override val warmupPeriodDays: Int = 0

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

    private fun calculateMaximumStakeAmount(): BigDecimal? {
        return vaults
            .mapNotNull { vault ->
                val availableCapacity = vault.capacity - vault.totalAssets
                if (availableCapacity > BigDecimal.ZERO) availableCapacity else null
            }
            .maxOrNull()
    }

    companion object {
        private const val MIN_COOLDOWN_DAYS = 1
        private const val MAX_COOLDOWN_DAYS = 4
        private val DEFAULT_MINIMUM_STAKE = BigDecimal("0.01")

        private const val TERMS_OF_SERVICE_URL = "https://www.p2p.org/terms-of-use"
        private const val PRIVACY_POLICY_URL = "https://www.p2p.org/privacy-policy"
    }
}