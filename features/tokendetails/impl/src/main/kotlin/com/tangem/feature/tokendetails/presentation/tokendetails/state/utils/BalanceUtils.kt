package com.tangem.feature.tokendetails.presentation.tokendetails.state.utils

import com.tangem.feature.tokendetails.presentation.tokendetails.state.BalanceType
import java.math.BigDecimal

fun BigDecimal.getBalance(selectedBalanceType: BalanceType, stakingAmount: BigDecimal?): BigDecimal {
    return if (selectedBalanceType == BalanceType.ALL && stakingAmount != null) {
        this.plus(stakingAmount)
    } else {
        this
    }
}
