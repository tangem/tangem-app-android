package com.tangem.domain.staking.model.stakekit.transaction

import com.tangem.domain.models.staking.YieldToken
import java.math.BigDecimal

data class StakingGasEstimate(
    val amount: BigDecimal,
    val token: YieldToken,
    val gasLimit: String?,
)