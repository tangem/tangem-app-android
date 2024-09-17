package com.tangem.domain.staking.model

import com.tangem.domain.staking.model.stakekit.BalanceType
import com.tangem.domain.staking.model.stakekit.Yield
import java.math.BigDecimal

data class PendingTransaction(
    val id: String,
    val type: BalanceType,
    val cryptoValue: String,
    val cryptoDecimal: BigDecimal,
    val rawCurrencyId: String?,
    val validator: Yield.Validator?,
)
