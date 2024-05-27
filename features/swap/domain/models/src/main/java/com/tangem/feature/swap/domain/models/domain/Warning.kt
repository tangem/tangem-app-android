package com.tangem.feature.swap.domain.models.domain

import java.math.BigDecimal

sealed class Warning {

    data class ExistentialDepositWarning(val existentialDeposit: BigDecimal) : Warning()

    data class MinAmountWarning(val dustValue: BigDecimal) : Warning()

    data class ReduceAmountWarning(val tezosFeeThreshold: BigDecimal) : Warning()

    sealed class Cardano : Warning() {

        data class MinAdaValueCharged(val tokenName: String, val minAdaValue: String) : Cardano()

        data object InsufficientBalanceToTransferCoin : Cardano()

        data class InsufficientBalanceToTransferToken(val tokenName: String) : Cardano()
    }
}
