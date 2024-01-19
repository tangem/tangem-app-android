package com.tangem.feature.wallet.presentation.wallet.viewmodels

import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.wallet.presentation.wallet.domain.getCardsCount
import com.tangem.feature.wallet.presentation.wallet.state.WalletLockedState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletCardState
import com.tangem.utils.Provider

/**
 * Resolver that determines which update action will be performed
 *
 * @property currentStateProvider         current state provider
 * @property getSelectedWalletSyncUseCase use case that returns selected wallet
 */
internal class WalletsUpdateActionResolver(
    private val currentStateProvider: Provider<WalletState>,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
) {

    fun resolve(wallets: List<UserWallet>): Action {
        val selectedWallet = wallets.getSelectedWallet()

        return when (val state = currentStateProvider()) {
            is WalletState.Initial -> {
                Action.Initialize(
                    selectedWalletIndex = wallets.indexOfWallet(id = selectedWallet.walletId),
                )
            }
            is WalletState.ContentState -> {
                getActionToUpdateContent(state = state, wallets = wallets, selectedWallet = selectedWallet)
            }
        }
    }

    private fun List<UserWallet>.getSelectedWallet(): UserWallet {
        val hasUnlockedWallet = any { !it.isLocked }
        return if (hasUnlockedWallet) {
            val selectedWalletId = getSelectedWalletSyncUseCase().fold(ifLeft = ::error, ifRight = UserWallet::walletId)

            firstOrNull { it.walletId == selectedWalletId }
                ?: error("Wallets don't contain a wallet with id: $selectedWalletId")
        } else {
            lastOrNull() ?: error("Wallets is empty")
        }
    }

    private fun getActionToUpdateContent(
        state: WalletState.ContentState,
        wallets: List<UserWallet>,
        selectedWallet: UserWallet,
    ): Action {
        return when {
            isWalletsCountChanged(state, wallets) -> {
                getActionToChangeWallets(state = state, wallets = wallets, selectedWallet = selectedWallet)
            }
            isSelectedWalletChanged(state, selectedWallet) -> {
                Action.Initialize(wallets.indexOfWallet(selectedWallet.walletId))
            }
            else -> getActionToUpdateCurrentWallet(state = state, wallets = wallets, selectedWallet = selectedWallet)
        }
    }

    private fun isWalletsCountChanged(state: WalletState.ContentState, wallets: List<UserWallet>): Boolean {
        val prevWalletsSize = state.walletsListConfig.wallets.size
        val walletsSize = wallets.size

        return prevWalletsSize != walletsSize
    }

    private fun getActionToChangeWallets(
        state: WalletState.ContentState,
        wallets: List<UserWallet>,
        selectedWallet: UserWallet,
    ): Action {
        val prevWalletsSize = state.walletsListConfig.wallets.size

        return when {
            prevWalletsSize > wallets.size -> {
                Action.DeleteWallet(
                    selectedWalletId = selectedWallet.walletId,
                    selectedWalletIndex = wallets.indexOfWallet(id = selectedWallet.walletId),
                    deletedWalletId = state.walletsListConfig.wallets.getDeletedWalletId(wallets),
                )
            }
            prevWalletsSize < wallets.size -> {
                Action.AddWallet(
                    selectedWalletIndex = wallets.indexOfWallet(id = selectedWallet.walletId),
                )
            }
            else -> Action.Unknown
        }
    }

    private fun List<WalletCardState>.getDeletedWalletId(wallets: List<UserWallet>): UserWalletId {
        return this
            .map(WalletCardState::id)
            .firstOrNull { !wallets.map(UserWallet::walletId).contains(it) }
            ?: error("Deleted wallet id is not found. Wallets contains all previous wallets ids")
    }

    private fun isSelectedWalletChanged(state: WalletState.ContentState, selectedWallet: UserWallet): Boolean {
        return state.getPrevSelectedWallet().id != selectedWallet.walletId
    }

    private fun getActionToUpdateCurrentWallet(
        state: WalletState.ContentState,
        wallets: List<UserWallet>,
        selectedWallet: UserWallet,
    ): Action {
        val selectedWalletName = selectedWallet.name
        val previousWalletState = state.getPrevSelectedWallet()
        return when {
            previousWalletState.title != selectedWalletName -> {
                Action.UpdateWalletName(selectedWalletName)
            }

            state is WalletLockedState && !selectedWallet.isLocked -> {
                Action.UnlockWallet(
                    selectedWalletIndex = wallets.indexOfWallet(id = selectedWallet.walletId),
                    selectedWallet = selectedWallet,
                    unlockedWallets = wallets.filterNot(UserWallet::isLocked),
                )
            }

            previousWalletState is WalletCardState.Content &&
                previousWalletState.cardCount != selectedWallet.getCardsCount() -> {
                Action.UpdateWalletCardCount
            }

            else -> Action.Unknown
        }
    }

    private fun WalletState.ContentState.getPrevSelectedWallet(): WalletCardState {
        val prevSelectedWalletIndex = walletsListConfig.selectedWalletIndex

        return walletsListConfig.wallets.getOrNull(prevSelectedWalletIndex)
            ?: error("Previous selected wallet is not found")
    }

    private fun List<UserWallet>.indexOfWallet(id: UserWalletId): Int {
        val selectedIndex = indexOfFirst { it.walletId == id }

        return if (selectedIndex == -1) {
            error("Wallets don't contain a wallet with id: $id")
        } else {
            selectedIndex
        }
    }

    sealed class Action {

        data class Initialize(val selectedWalletIndex: Int) : Action()

        data class UpdateWalletName(val name: String) : Action()

        data class UnlockWallet(
            val selectedWalletIndex: Int,
            val selectedWallet: UserWallet,
            val unlockedWallets: List<UserWallet>,
        ) : Action()

        data class DeleteWallet(
            val selectedWalletId: UserWalletId,
            val selectedWalletIndex: Int,
            val deletedWalletId: UserWalletId,
        ) : Action()

        data class AddWallet(val selectedWalletIndex: Int) : Action()

        object UpdateWalletCardCount : Action()

        object Unknown : Action()
    }
}