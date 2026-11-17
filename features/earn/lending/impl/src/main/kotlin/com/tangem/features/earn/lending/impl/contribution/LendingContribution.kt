package com.tangem.features.earn.lending.impl.contribution

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.balance.BalanceContribution
import java.math.BigDecimal

/** The position can't be sent before it is withdrawn, so it adds nothing to the spendable amount */
internal data class LendingContribution(
    val amount: BigDecimal,
    override val source: StatusSource,
) : BalanceContribution {

    override fun totalDeltaCryptoAmount(): BigDecimal = amount
}