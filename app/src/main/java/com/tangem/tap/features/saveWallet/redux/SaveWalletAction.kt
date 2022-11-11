package com.tangem.tap.features.saveWallet.redux

import com.tangem.common.core.TangemError
import org.rekotlin.Action

internal sealed interface SaveWalletAction : Action {
    object Save : SaveWalletAction {
        object Success : SaveWalletAction
        data class Error(val error: TangemError) : SaveWalletAction
    }

    object Dismiss : SaveWalletAction

    object CloseError : SaveWalletAction

    object SaveWalletWasShown : SaveWalletAction
}