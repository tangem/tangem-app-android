package com.tangem.tap.features.walletSelector.redux

import com.tangem.common.core.TangemError
import org.rekotlin.Action

internal sealed interface WalletSelectorAction : Action {
    object UnlockWithBiometry : WalletSelectorAction {
        object Success : WalletSelectorAction
        data class Error(val error: TangemError) : WalletSelectorAction
    }

    data class SelectWallet(
        val walletId: String,
    ) : WalletSelectorAction

    data class RenameWallet(
        val walletId: String,
        val newName: String,
    ) : WalletSelectorAction

    data class RemoveWallets(
        val walletIdsToRemove: List<String>,
    ) : WalletSelectorAction

    object AddWallet : WalletSelectorAction {
        object Success : WalletSelectorAction
        data class Error(val error: TangemError) : WalletSelectorAction
    }

    object CloseError : WalletSelectorAction
}