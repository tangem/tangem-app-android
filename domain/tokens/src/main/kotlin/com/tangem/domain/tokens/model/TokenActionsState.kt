package com.tangem.domain.tokens.model

import com.tangem.domain.wallets.models.UserWalletId

data class TokenActionsState(
    val walletId: UserWalletId,
    val cryptoCurrencyStatus: CryptoCurrencyStatus,
    val states: List<ActionState>,
) {

    sealed class ActionState {

        abstract val disabledReason: ButtonDisabledReason

        data class Buy(override val disabledReason: ButtonDisabledReason) : ActionState()

        data class CopyAddress(override val disabledReason: ButtonDisabledReason) : ActionState()

        data class Sell(override val disabledReason: ButtonDisabledReason) : ActionState()

        data class Receive(override val disabledReason: ButtonDisabledReason) : ActionState()

        data class Swap(override val disabledReason: ButtonDisabledReason) : ActionState()

        data class Send(override val disabledReason: ButtonDisabledReason) : ActionState()

        data class HideToken(override val disabledReason: ButtonDisabledReason) : ActionState()
    }
}
