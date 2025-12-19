package com.tangem.domain.staking.model

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.staking.YieldToken
import com.tangem.domain.staking.model.common.RewardClaiming
import com.tangem.domain.staking.model.common.RewardSchedule
import com.tangem.domain.staking.model.common.StakingActionArgs
import java.math.BigDecimal

/**
 * Strategy interface for staking integrations.
 * Abstracts over StakeKit and P2PEthPool staking providers.
 */
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

    val enterArgs: StakingActionArgs?

    val exitArgs: StakingActionArgs?

    // Metadata

    val warmupPeriodDays: Int

    val cooldownPeriodDays: Int?

    val rewardSchedule: RewardSchedule

    val rewardClaiming: RewardClaiming

    // Basic

    fun getCurrentToken(rawCurrencyId: CryptoCurrency.RawID?): YieldToken
}