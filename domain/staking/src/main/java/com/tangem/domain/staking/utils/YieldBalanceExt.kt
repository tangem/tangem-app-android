package com.tangem.domain.staking.utils

import com.tangem.domain.models.staking.BalanceType
import com.tangem.domain.models.staking.YieldBalance
import com.tangem.lib.crypto.BlockchainUtils
import java.math.BigDecimal

fun YieldBalance.Data.getTotalWithRewardsStakingBalance(blockchainId: String): BigDecimal {
    return if (BlockchainUtils.isIncludeStakingTotalBalance(blockchainId = blockchainId)) {
        balance.items.sumOf { it.amount }
    } else {
        getRewardStakingBalance()
    }
}

fun YieldBalance.Data.getTotalStakingBalance(blockchainId: String): BigDecimal {
    return if (BlockchainUtils.isIncludeStakingTotalBalance(blockchainId = blockchainId)) {
        balance.items
            .filterNot { it.type == BalanceType.REWARDS }
            .sumOf { it.amount }
    } else {
        balance.items
            .filterNot { it.type == BalanceType.REWARDS }
            .sumOf { it.amount } - getRewardStakingBalance()
    }
}

fun YieldBalance.Data.getRewardStakingBalance(): BigDecimal {
    return balance.items
        .filter { it.type == BalanceType.REWARDS }
        .sumOf { it.amount }
}

fun YieldBalance.Data.getValidatorsCount(): Int {
    return balance.items
        .filterNot { it.validatorAddress.isNullOrBlank() }
        .distinctBy { it.validatorAddress }
        .size
}