package com.tangem.features.tangempay.withdraw.bank.model

import com.tangem.domain.pay.model.bankofframp.AmountLimits
import com.tangem.domain.pay.model.bankofframp.OfframpDestination
import kotlinx.collections.immutable.ImmutableList
import java.util.Currency

/**
 * Everything the wizard needs to price a withdrawal, prepared by the hub before the flow starts. Both lists are
 * non-empty: every source is a token the off-ramp accepts and the account holds, every destination a rail it offers.
 */
internal data class BankWithdrawOptions(
    val sources: ImmutableList<BankWithdrawSource>,
    val destinations: ImmutableList<OfframpDestination>,
    val limits: AmountLimits,
    val currency: Currency,
)