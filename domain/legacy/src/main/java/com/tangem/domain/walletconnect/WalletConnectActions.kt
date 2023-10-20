package com.tangem.domain.walletconnect

import com.tangem.domain.wallets.models.UserWallet
import org.rekotlin.Action

sealed class WalletConnectActions : Action {
    sealed class New {
        data class Initialize(val userWallet: UserWallet) : WalletConnectActions()

        data class SetupUserChains(val userWallet: UserWallet) : WalletConnectActions()
    }
}