package com.tangem.features.tangempay.withdraw.bank.model

import com.tangem.domain.tangempay.model.AmountLimits
import com.tangem.domain.tangempay.model.bankofframp.OfframpDestination
import kotlinx.collections.immutable.ImmutableList
import java.util.Currency

/**
 * Everything the wizard needs to price a withdrawal, prepared by the hub before the flow starts. Both lists are
 * non-empty: [sources] lists every token the account holds a balance in, at least one of them available, and
 * [destinations] every fixed-fee ACH or wire rail the off-ramp offers.
 *
 * A withdrawal must fall within [amountLimitsFor] the chosen rail; inside that range the selected token's balance
 * caps it too.
 */
internal data class BankWithdrawOptions(
    val sources: ImmutableList<BankWithdrawSource>,
    val destinations: ImmutableList<OfframpDestination>,
    val currency: Currency,
    val defaultAmountLimits: AmountLimits,
    val isLimitedPerRail: Boolean,
) {

    fun amountLimitsFor(destination: OfframpDestination): AmountLimits {
        val limits = destination.amountLimits?.takeIf { isLimitedPerRail } ?: return defaultAmountLimits
        return limits.takeIf { it.currency.equals(currency.currencyCode, ignoreCase = true) } ?: defaultAmountLimits
    }
}