package com.tangem.features.tangempay.withdraw.hub.navigation

import com.tangem.core.decompose.navigation.Route
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawOptions
import kotlinx.coroutines.flow.StateFlow

/**
 * Screens hosted by the withdraw hub. The stack is deliberately not persisted: the wizard runs on the options the hub
 * prepared and on an in-memory draft, neither of which survives process death, so a restored app starts at [Hub].
 * That is also why [BankWithdraw] may carry the options themselves instead of a key to look them up by.
 */
internal sealed class TangemPayWithdrawHubRoute : Route {

    data object Hub : TangemPayWithdrawHubRoute()

    data class BankWithdraw(
        val entry: BankWithdrawEntry,
        val options: StateFlow<BankWithdrawOptions>,
    ) : TangemPayWithdrawHubRoute()
}