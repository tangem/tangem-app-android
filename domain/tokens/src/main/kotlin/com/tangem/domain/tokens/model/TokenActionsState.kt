package com.tangem.domain.tokens.model

import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId

data class TokenActionsState(
    val walletId: UserWalletId,
    val cryptoCurrencyId: CryptoCurrency.ID,
    val states: List<ActionState>,
) {

    sealed class ActionState {

        abstract val enabled: Boolean

        data class Buy(override val enabled: Boolean) : ActionState()

        data class Sell(override val enabled: Boolean) : ActionState()

        data class Receive(override val enabled: Boolean) : ActionState()

        data class Swap(override val enabled: Boolean) : ActionState()

        data class Send(override val enabled: Boolean) : ActionState()
    }
}