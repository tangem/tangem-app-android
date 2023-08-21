package com.tangem.domain.tokens.model

import com.tangem.domain.wallets.models.UserWalletId

data class WalletActionsState(
    val walletId: UserWalletId,
    val states: List<ActionState>,
) {

    sealed class ActionState {

        abstract val enabled: Boolean

        data class Buy(override val enabled: Boolean) : ActionState()

        data class Sell(override val enabled: Boolean) : ActionState()

        data class Receive(override val enabled: Boolean) : ActionState()

        data class Exchange(override val enabled: Boolean) : ActionState()

    }

}

