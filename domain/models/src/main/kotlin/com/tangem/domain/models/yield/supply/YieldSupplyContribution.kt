package com.tangem.domain.models.yield.supply

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.balance.BalanceContribution
import java.math.BigDecimal

/**
 * Yield supply seen as a [BalanceContribution].
 *
 * A wrapper rather than making [YieldSupplyStatus] itself a contribution: the status is assembled from blockchain
 * SDK data and carries no freshness of its own, so [source] has to come from what delivered it: the network status
 * or the Yield Module position.
 *
 * @property status the wrapped status — the partition and APY information ("Axis 2") lives here and is reached by
 * downcasting to this type.
 * @property spendableDelta what makes the network amount equal wallet + supplied, resolved by the provider against the
 * network status it was handed. Zero while the network amount already holds the supplied funds (the SDK reported the
 * module's `effectiveBalance`). Otherwise the difference to the wallet + supplied total that the Yield Module read
 * together with the supplied part — not the supplied part alone, so a network amount observed after funds moved
 * between the wallet and the module can't count them twice. Negative when the network amount is ahead of that read.
 */
data class YieldSupplyContribution(
    val status: YieldSupplyStatus,
    override val source: StatusSource,
    val spendableDelta: BigDecimal = BigDecimal.ZERO,
) : BalanceContribution {

    override val kind: String get() = CONTRIBUTION_KIND

    /**
     * Always zero: supplied funds are spendable, so they reach the total through `amount` — either inside the network
     * amount or folded into it from [spendableDeltaCryptoAmount] — never on top of it.
     */
    override fun totalDeltaCryptoAmount(): BigDecimal = BigDecimal.ZERO

    /**
     * [spendableDelta] while yield supply is active: the module withdraws supplied funds on send, so they are
     * spendable. Zero when it is not active.
     */
    override fun spendableDeltaCryptoAmount(): BigDecimal = if (status.isActive) spendableDelta else BigDecimal.ZERO

    companion object {

        /** [BalanceContribution.kind] of every yield-supply contribution. Owned here, not by the base contract. */
        const val CONTRIBUTION_KIND = "yield_supply"
    }
}