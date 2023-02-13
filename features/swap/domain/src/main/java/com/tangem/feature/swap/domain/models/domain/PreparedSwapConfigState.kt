package com.tangem.feature.swap.domain.models.domain

data class PreparedSwapConfigState(
    val isAllowedToSpend: Boolean,
    val isBalanceEnough: Boolean,
    val isFeeEnough: Boolean,
)
