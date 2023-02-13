package com.tangem.tap.features.walletSelector.redux

import com.tangem.domain.common.CardDTO
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.model.TotalFiatBalance
import com.tangem.tap.domain.model.UserWallet
import org.rekotlin.Action

internal object WalletSelectorReducer {
    fun reduce(action: Action, state: AppState): WalletSelectorState {
        return if (action is WalletSelectorAction) {
            internalReduce(action, state.walletSelectorState)
        } else state.walletSelectorState
    }

    @Suppress("ComplexMethod")
    private fun internalReduce(action: WalletSelectorAction, state: WalletSelectorState): WalletSelectorState {
        return when (action) {
            is WalletSelectorAction.UserWalletsLoaded -> state.copy(
                wallets = action.userWallets.updateWalletsModels(state.wallets),
            )
            is WalletSelectorAction.SelectedWalletChanged -> state.copy(
                selectedWalletId = action.selectedWallet.walletId,
            )
            is WalletSelectorAction.IsLockedChanged -> state.copy(
                isLocked = action.isLocked,
            )
            is WalletSelectorAction.BalancesLoaded -> state.copy(
                wallets = action.userWalletModels,
            )
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
                .find { it.id == userWallet.walletId }
                ?.let {
                    it.copy(
                        name = userWallet.name,
                        artworkUrl = userWallet.artworkUrl,
                        isLocked = userWallet.isLocked,
                        type = userWallet.getType(prevType = it.type),
                    )
                }
                ?: with(userWallet) {
                    UserWalletModel(
                        id = walletId,
                        name = name,
                        artworkUrl = artworkUrl,
                        type = getType(),
                        fiatBalance = TotalFiatBalance.Loading,
                        isLocked = isLocked,
                    )
                }
        }
    }

    private fun UserWallet.getType(prevType: UserWalletModel.Type? = null): UserWalletModel.Type {
        return if (isMultiCurrency) {
            UserWalletModel.Type.MultiCurrency(
                cardsInWallet = (scanResponse.card.backupStatus as? CardDTO.BackupStatus.Active)
                    ?.cardCount?.inc()
                    ?: 1,
                tokensCount = (prevType as? UserWalletModel.Type.MultiCurrency)?.tokensCount ?: 0,
            )
        } else {
            UserWalletModel.Type.SingleCurrency(
                blockchainName = scanResponse.cardTypesResolver.getBlockchain().fullName,
            )
        }
    }
}
