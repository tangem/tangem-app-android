package com.tangem.tap.features.walletSelector.redux

import com.tangem.domain.common.CardDTO
import com.tangem.tap.common.extensions.replaceByOrAdd
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.extensions.isMultiwalletAllowed
import com.tangem.tap.domain.model.TotalFiatBalance
import com.tangem.tap.domain.model.UserWallet
import org.rekotlin.Action

internal object WalletSelectorReducer {
    fun reduce(action: Action, state: AppState): WalletSelectorState {
        return if (action is WalletSelectorAction) {
            internalReduce(action, state.walletSelectorState)
        } else state.walletSelectorState
    }

    private fun internalReduce(action: WalletSelectorAction, state: WalletSelectorState): WalletSelectorState {
        return when (action) {
            is WalletSelectorAction.UserWalletsLoaded -> state.copy(
                wallets = action.userWallets.updateWalletsModels(state.wallets),
            )
            is WalletSelectorAction.SelectedWalletChanged -> state.copy(
                selectedWalletId = action.selectedWallet.walletId.stringValue,
            )
            is WalletSelectorAction.IsLockedChanged -> state.copy(
                isLocked = action.isLocked,
            )
            is WalletSelectorAction.BalanceLoaded -> state.copy(
                wallets = state.wallets.updateWithBalance(action.userWalletModel),
            )
            is WalletSelectorAction.HandleError -> state.copy(error = action.error)
            is WalletSelectorAction.CloseError -> state.copy(error = null)
            is WalletSelectorAction.UnlockWithBiometry -> state.copy(
                isUnlockInProgress = true,
            )
            is WalletSelectorAction.UnlockWithBiometry.Error -> state.copy(
                isUnlockInProgress = false,
                error = action.error,
            )
            is WalletSelectorAction.UnlockWithBiometry.Success -> state.copy(
                isUnlockInProgress = false,
            )
            is WalletSelectorAction.AddWallet -> state.copy(
                isCardSavingInProgress = true,
            )
            is WalletSelectorAction.AddWallet.Error -> state.copy(
                isCardSavingInProgress = false,
                error = action.error,
            )
            is WalletSelectorAction.AddWallet.Success -> state.copy(
                isCardSavingInProgress = false,
            )
            is WalletSelectorAction.ChangeAppCurrency -> state.copy(
                fiatCurrency = action.fiatCurrency,
            )
            is WalletSelectorAction.WalletStoresChanged,
            is WalletSelectorAction.SelectWallet,
            is WalletSelectorAction.RemoveWallets,
            is WalletSelectorAction.RenameWallet,
            -> state
        }
    }

    private fun List<UserWallet>.updateWalletsModels(prevWallets: List<UserWalletModel>): List<UserWalletModel> {
        return this.map { userWallet ->
            prevWallets
                .find { it.id == userWallet.walletId.stringValue }
                ?.copy(
                    name = userWallet.name,
                    artworkUrl = userWallet.artworkUrl,
                )
                ?: with(userWallet) {
                    UserWalletModel(
                        id = walletId.stringValue,
                        name = name,
                        artworkUrl = artworkUrl,
                        type = getType(),
                        fiatBalance = TotalFiatBalance.Loading,
                    )
                }
        }
    }

    private fun List<UserWalletModel>.updateWithBalance(
        userWalletModel: UserWalletModel,
    ): List<UserWalletModel> {
        return ArrayList(this).apply {
            replaceByOrAdd(userWalletModel) { it.id == userWalletModel.id }
        }
    }

    private fun UserWallet.getType(): UserWalletModel.Type {
        return if (scanResponse.card.isMultiwalletAllowed) {
            UserWalletModel.Type.MultiCurrency(
                tokensCount = 0,
                cardsInWallet = (scanResponse.card.backupStatus as? CardDTO.BackupStatus.Active)
                    ?.cardCount?.inc()
                    ?: 1,
            )
        } else {
            UserWalletModel.Type.SingleCurrency(
                blockchainName = null,
            )
        }
    }
}
