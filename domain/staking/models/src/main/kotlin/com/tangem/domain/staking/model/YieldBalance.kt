package com.tangem.domain.staking.model

import java.math.BigDecimal

data class YieldBalance(
    val type: BalanceType,
    val amount: BigDecimal,
    val pricePerShare: BigDecimal,
)

enum class BalanceType {
    AVAILABLE,
    STAKED,
    UNSTAKING,
    UNSTAKED,
    PREPARING,
    REWARDS,
    LOCKED,
    UNLOCKING,
    UNKNOWN,
}