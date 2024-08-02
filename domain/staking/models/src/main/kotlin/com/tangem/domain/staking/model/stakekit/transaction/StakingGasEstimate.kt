package com.tangem.domain.staking.model.stakekit.transaction

import com.tangem.domain.staking.model.stakekit.Token
import java.math.BigDecimal

data class StakingGasEstimate(
    val amount: BigDecimal,
    val token: Token,
    val gasLimit: String?,
)