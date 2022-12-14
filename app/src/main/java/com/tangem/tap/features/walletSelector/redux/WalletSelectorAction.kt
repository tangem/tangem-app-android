package com.tangem.tap.features.walletSelector.redux

import com.tangem.common.core.TangemError
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.domain.model.WalletStoreModel
import org.rekotlin.Action

internal sealed interface WalletSelectorAction : Action {
    data class UserWalletsLoaded(
        val userWallets: List<UserWallet>,
    ) : WalletSelectorAction

    data class SelectedWalletChanged(
        val selectedWallet: UserWallet,
    ) : WalletSelectorAction

    data class IsLockedChanged(
        val isLocked: Boolean,
    ) : WalletSelectorAction

    data class WalletStoresChanged(
        val walletsStores: Map<UserWalletId, List<WalletStoreModel>>,
    ) : WalletSelectorAction

    data class BalanceLoaded(
        val userWalletModel: UserWalletModel,
    ) : WalletSelectorAction

    object UnlockWithBiometry : WalletSelectorAction {
        object Success : WalletSelectorAction
        data class Error(val error: TangemError) : WalletSelectorAction
    }

    data class SelectWallet(
        val walletId: String,
    ) : WalletSelectorAction

    data class UnlockWalletWithCard(
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

    data class ChangeAppCurrency(
        val fiatCurrency: FiatCurrency,
    ) : WalletSelectorAction

    data class HandleError(val error: TangemError) : WalletSelectorAction

    object CloseError : WalletSelectorAction
}
