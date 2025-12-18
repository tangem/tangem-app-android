package com.tangem.domain.staking.model

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.staking.YieldToken
import com.tangem.domain.staking.model.stakekit.Yield
import java.math.BigDecimal

/**
 * Strategy interface for staking integrations.
 * Abstracts over StakeKit and P2PEthPool staking providers.
 */
// TODO p2p get rid of stakekit-specific models in StakingIntegration and implementors
interface StakingIntegration {

    // Basic

    val integrationId: StakingIntegrationID

    val token: YieldToken

    val tokens: List<YieldToken>

    // Targets (validators or vaults)

    val targets: List<StakingTarget>

    val preferredTargets: List<StakingTarget>

    val areAllTargetsFull: Boolean

    // Enter/Exit Args

    val isPartialAmountDisabled: Boolean

    val enterMinimumAmount: BigDecimal?

    val exitMinimumAmount: BigDecimal?

    val enterArgs: Yield.Args.Enter?

    val exitArgs: Yield.Args.Enter?

    // Metadata

    val warmupPeriodDays: Int

    val cooldownPeriodDays: Int?

    val rewardSchedule: Yield.Metadata.RewardSchedule

    val rewardClaiming: Yield.Metadata.RewardClaiming

    // Basic

    fun getCurrentToken(rawCurrencyId: CryptoCurrency.RawID?): YieldToken
}