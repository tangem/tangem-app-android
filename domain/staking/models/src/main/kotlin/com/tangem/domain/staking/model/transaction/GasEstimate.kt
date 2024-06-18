package com.tangem.domain.staking.model.transaction

import com.tangem.domain.staking.model.Token
import java.math.BigDecimal

data class GasEstimate(
    val amount: BigDecimal,
    val token: Token,
    val gasLimit: BigDecimal,
)
