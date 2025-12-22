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
    vaults: List<P2PEthPoolVault>,
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
            maximum = null,
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

    override val cooldownPeriodDays: Int = DEFAULT_COOLDOWN_DAYS

    override val rewardSchedule: RewardSchedule = RewardSchedule.DAY

    override val rewardClaiming: RewardClaiming = RewardClaiming.AUTO

    override fun getCurrentToken(rawCurrencyId: CryptoCurrency.RawID?): YieldToken = token

    companion object {
        private const val DEFAULT_COOLDOWN_DAYS = 7
        private val DEFAULT_MINIMUM_STAKE = BigDecimal("0.01")
    }
}