package com.tangem.features.tangempay.withdraw.hub.model

import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawOptions

/** Raw inputs of the withdraw hub; the UM factory turns them into the swap and bank sections. */
internal data class WithdrawHubState(
    val options: OptionsState = OptionsState.Loading,
    val recents: RecentsState = RecentsState.Loading,
)

internal sealed interface OptionsState {

    data object Loading : OptionsState

    /** The off-ramp is out of reach: the request failed, no rail is offered, or no held token is accepted. */
    data object Unavailable : OptionsState

    data class Ready(val options: BankWithdrawOptions) : OptionsState
}

internal sealed interface RecentsState {

    data object Loading : RecentsState

    data object Failed : RecentsState

    /** [hasSavedAccounts] gates "See all" when the usage log is empty but recipients exist elsewhere. */
    data class Loaded(val accounts: List<BankAccount>, val hasSavedAccounts: Boolean) : RecentsState
}