package com.tangem.domain.staking.model

import com.tangem.domain.staking.model.stakekit.BalanceType
import com.tangem.domain.staking.model.stakekit.Token
import com.tangem.domain.staking.model.stakekit.Yield
import java.math.BigDecimal

data class PendingTransaction(
    val groupId: String?,
    val token: Token,
    val type: BalanceType,
    val amount: BigDecimal,
    val rawCurrencyId: String?,
    val validator: Yield.Validator?,
    val balancesId: Int,
)
