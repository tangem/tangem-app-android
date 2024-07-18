package com.tangem.domain.staking.model.transaction

import com.tangem.domain.staking.model.Token
import java.math.BigDecimal

data class StakingGasEstimate(
    val amount: BigDecimal,
    val token: Token,
    val gasLimit: String?,
)