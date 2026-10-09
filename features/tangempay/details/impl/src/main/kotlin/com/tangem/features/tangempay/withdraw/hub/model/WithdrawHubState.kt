package com.tangem.features.tangempay.withdraw.hub.model

import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.recents.RecentTokenTransfer
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawOptions

/** Raw inputs of the withdraw hub; the UM factory turns them into the swap and bank sections. */
internal data class WithdrawHubState(
    val options: OptionsState = OptionsState.Loading,
    val recents: RecentsState = RecentsState.Loading,
    val tokenRecents: TokenRecentsState = TokenRecentsState.Loading,
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

    /** [hasMoreAccounts] gates "See all": it would only repeat [accounts] when they are every saved recipient. */
    data class Loaded(val accounts: List<BankAccount>, val hasMoreAccounts: Boolean) : RecentsState
}

internal sealed interface TokenRecentsState {

    data object Loading : TokenRecentsState

    data class Loaded(val transfers: List<RecentTokenTransfer>) : TokenRecentsState
}