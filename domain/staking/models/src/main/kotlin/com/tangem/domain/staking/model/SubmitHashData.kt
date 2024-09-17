package com.tangem.domain.staking.model

import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransactionType
import java.math.BigDecimal

data class SubmitHashData(
    val transactionHash: String,
    val transactionId: String,
    val validator: Yield.Validator?,
    val amount: BigDecimal,
    val type: StakingTransactionType,
)
