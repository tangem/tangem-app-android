package com.tangem.domain.staking.model

import com.tangem.domain.staking.model.stakekit.BalanceItem
import com.tangem.domain.staking.model.stakekit.BalanceType
import com.tangem.domain.staking.model.stakekit.Yield
import java.math.BigDecimal

data class PendingTransaction(
    val groupId: String?,
    val type: BalanceType,
    val amount: BigDecimal,
    val rawCurrencyId: String?,
    val validator: Yield.Validator?,
    val balancesId: Int,
    val balanceItems: List<BalanceItem>, // TODO staking remove it
)