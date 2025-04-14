package com.tangem.feature.tokendetails.presentation.tokendetails.state.utils

import com.tangem.feature.tokendetails.presentation.tokendetails.state.BalanceType
import java.math.BigDecimal

internal fun BigDecimal.getBalance(
    selectedBalanceType: BalanceType,
    stakingAmount: BigDecimal?,
    includeStaking: Boolean,
): BigDecimal {
    return if (selectedBalanceType == BalanceType.ALL && stakingAmount != null && includeStaking) {
        this.plus(stakingAmount)
    } else {
        this
    }
}