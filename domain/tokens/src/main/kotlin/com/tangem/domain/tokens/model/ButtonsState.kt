package com.tangem.domain.tokens.model

import com.tangem.domain.wallets.models.UserWalletId

data class WalletButtonsState(
    val walletId: UserWalletId,
    val states: List<ButtonState>,
)

sealed class ButtonState {

    abstract val enabled: Boolean

    class Buy(override val enabled: Boolean) : ButtonState()

    class Sell(override val enabled: Boolean) : ButtonState()

    class Receive(override val enabled: Boolean) : ButtonState()

    class Exchange(override val enabled: Boolean) : ButtonState()

}