package com.tangem.domain.staking.model

import java.math.BigDecimal

sealed class YieldBalance {

    data class Data(
        val balance: YieldBalanceItem,
    ) : YieldBalance()

    data object Empty : YieldBalance()
}

data class YieldBalanceItem(
    val items: List<BalanceItem>,
    val integrationId: String?,
)

data class BalanceItem(
    val type: BalanceType,
    val amount: BigDecimal,
    val pricePerShare: BigDecimal,
    val rawCurrencyId: String?,
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