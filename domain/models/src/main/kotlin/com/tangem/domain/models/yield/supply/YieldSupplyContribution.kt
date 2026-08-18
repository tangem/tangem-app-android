package com.tangem.domain.models.yield.supply

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.balance.BalanceContribution
import java.math.BigDecimal

/**
 * Yield supply seen as a [BalanceContribution].
 *
 * A wrapper rather than making [YieldSupplyStatus] itself a contribution: the status is assembled from blockchain
 * SDK data and carries no freshness of its own, so [source] has to come from the network status it arrived with.
 *
 * @property status the wrapped status — the partition and APY information ("Axis 2") lives here and is reached by
 * downcasting to this type.
 */
data class YieldSupplyContribution(
    val status: YieldSupplyStatus,
    override val source: StatusSource,
) : BalanceContribution {

    override val kind: String get() = CONTRIBUTION_KIND

    /**
     * Always zero: the supplied principal is **already inside** the network amount. Yield supply only partitions
     * the displayed balance into supplied / idle and accrues an APY ticker, it never adds to a total.
     */
    override fun totalDeltaCryptoAmount(): BigDecimal = BigDecimal.ZERO

    companion object {

        /** [BalanceContribution.kind] of every yield-supply contribution. Owned here, not by the base contract. */
        const val CONTRIBUTION_KIND = "yield_supply"
    }
}