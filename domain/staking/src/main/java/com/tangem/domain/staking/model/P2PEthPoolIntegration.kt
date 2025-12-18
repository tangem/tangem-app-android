package com.tangem.domain.staking.model

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.staking.YieldToken
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import com.tangem.domain.staking.model.stakekit.Yield
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

    override val enterArgs: Yield.Args.Enter? = null

    override val exitArgs: Yield.Args.Enter? = null

    // Metadata

    override val warmupPeriodDays: Int = 0

    override val cooldownPeriodDays: Int = DEFAULT_COOLDOWN_DAYS

    override val rewardSchedule: Yield.Metadata.RewardSchedule = Yield.Metadata.RewardSchedule.DAY

    override val rewardClaiming: Yield.Metadata.RewardClaiming = Yield.Metadata.RewardClaiming.AUTO

    override fun getCurrentToken(rawCurrencyId: CryptoCurrency.RawID?): YieldToken = token

    companion object {
        private const val DEFAULT_COOLDOWN_DAYS = 7
        private val DEFAULT_MINIMUM_STAKE = BigDecimal("0.01")
    }
}