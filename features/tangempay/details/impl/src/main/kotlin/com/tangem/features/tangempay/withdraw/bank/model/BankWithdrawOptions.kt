package com.tangem.features.tangempay.withdraw.bank.model

import com.tangem.domain.pay.model.bankofframp.OfframpDestination
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal
import java.util.Currency

/**
 * Everything the wizard needs to price a withdrawal, prepared by the hub before the flow starts. Both lists are
 * non-empty: [sources] lists every token the account holds a balance in, at least one of them available, and
 * [destinations] every rail the off-ramp offers.
 *
 * A withdrawal must fall within [minAmount]..[maxAmount]; inside that range the selected token's balance caps it too.
 */
internal data class BankWithdrawOptions(
    val sources: ImmutableList<BankWithdrawSource>,
    val destinations: ImmutableList<OfframpDestination>,
    val minAmount: BigDecimal,
    val maxAmount: BigDecimal,
    val currency: Currency,
)