package com.tangem.features.tangempay.withdraw.bank.model

import com.tangem.domain.tangempay.model.AmountLimits
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.domain.tangempay.model.bankofframp.OfframpDestination
import kotlinx.collections.immutable.ImmutableList
import java.util.Currency

/**
 * Everything the wizard needs to price a withdrawal, prepared by the hub before the flow starts. Both lists are
 * non-empty: [sources] lists every token the account holds a balance in, at least one of them available, and
 * [destinations] every rail the account can take: fixed-fee ACH and wire, and SPEI where it is offered.
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

    fun destinationsFor(recipientRail: BankRail?): List<OfframpDestination> {
        val compatible = if (recipientRail == BankRail.SPEI) {
            destinations.filter { it.rail == BankRail.SPEI }
        } else {
            destinations.filterNot { it.rail == BankRail.SPEI }
        }
        return compatible.ifEmpty { destinations }
    }

    fun amountLimitsFor(destination: OfframpDestination): AmountLimits {
        val limits = destination.amountLimits?.takeIf { isLimitedPerRail } ?: return defaultAmountLimits
        return limits.takeIf { it.currency.equals(currency.currencyCode, ignoreCase = true) } ?: defaultAmountLimits
    }
}