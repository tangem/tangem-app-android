package com.tangem.common

import com.tangem.domain.models.staking.BalanceType
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.lib.crypto.BlockchainUtils
import java.math.BigDecimal

/**
 * Provider-agnostic extension to get total balance including rewards.
 * Works for both StakeKit and P2PEthPool providers.
 *
 * Returns sum of all staking-related balances including rewards
 * (staked + unstaking + withdrawable + rewards). For P2PEthPool the staked amount already includes the
 * auto-compounded rewards, so they are not added again.
 *
 * When [BlockchainUtils.isIncludeStakingTotalBalance] is false, the staked balance
 * is already included in the main wallet balance, so we only return rewards.
 */
fun StakingBalance.Data.getTotalWithRewardsStakingBalance(blockchainId: String): BigDecimal {
    return when (this) {
        is StakingBalance.Data.StakeKit -> getTotalWithRewardsStakingBalanceStakeKit(blockchainId)
        is StakingBalance.Data.P2PEthPool -> {
            if (BlockchainUtils.isIncludeStakingTotalBalance(blockchainId)) {
                totalStaked + unstakingAmount + withdrawableAmount
            } else {
                totalRewards
            }
        }
    }
}

/**
 * Provider-agnostic extension to get total staking balance excluding rewards.
 * Works for both StakeKit and P2PEthPool providers.
 *
 * Returns sum of all staking-related balances (staked + unstaking + withdrawable)
 * excluding rewards.
 */
fun StakingBalance.Data.getTotalStakingBalance(blockchainId: String): BigDecimal {
    return when (this) {
        is StakingBalance.Data.StakeKit -> getTotalStakingBalanceStakeKit(blockchainId)
        is StakingBalance.Data.P2PEthPool -> totalStaked + unstakingAmount + withdrawableAmount
    }
}

/**
 * StakeKit-specific extension to get total balance including rewards.
 */
private fun StakingBalance.Data.StakeKit.getTotalWithRewardsStakingBalanceStakeKit(blockchainId: String): BigDecimal {
    return if (BlockchainUtils.isIncludeStakingTotalBalance(networkId = blockchainId)) {
        balance.items.sumOf { it.amount }
    } else {
        getRewardStakingBalance()
    }
}

/**
 * StakeKit-specific extension to get total staked balance excluding rewards.
 */
private fun StakingBalance.Data.StakeKit.getTotalStakingBalanceStakeKit(blockchainId: String): BigDecimal {
    return if (BlockchainUtils.isIncludeStakingTotalBalance(networkId = blockchainId)) {
        balance.items
            .filterNot { it.type == BalanceType.REWARDS }
            .sumOf { it.amount }
    } else {
        balance.items
            .filterNot { it.type == BalanceType.REWARDS }
            .sumOf { it.amount } - getRewardStakingBalance()
    }
}

/**
 * StakeKit-specific extension to get reward balance.
 */
fun StakingBalance.Data.StakeKit.getRewardStakingBalance(): BigDecimal {
    return balance.items
        .filter { it.type == BalanceType.REWARDS }
        .sumOf { it.amount }
}

/**
 * StakeKit-specific extension to get validators count.
 */
fun StakingBalance.Data.StakeKit.getValidatorsCount(): Int {
    return balance.items
        .filterNot { it.validatorAddress.isNullOrBlank() }
        .distinctBy { it.validatorAddress }
        .size
}