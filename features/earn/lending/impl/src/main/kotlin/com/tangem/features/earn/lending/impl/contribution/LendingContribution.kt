package com.tangem.features.earn.lending.impl.contribution

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.balance.AccruingBalanceContribution
import java.math.BigDecimal

/**
 * The position can't be sent before it is withdrawn, so it adds nothing to the spendable amount.
 *
 * @property accrualRate net of the performance fee; zero while unknown
 */
internal data class LendingContribution(
    val amount: BigDecimal,
    override val source: StatusSource,
    override val accrualRate: BigDecimal = BigDecimal.ZERO,
) : AccruingBalanceContribution {

    override fun totalDeltaCryptoAmount(): BigDecimal = amount
}