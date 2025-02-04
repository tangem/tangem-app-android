package com.tangem.feature.wallet.presentation.wallet.viewmodels

import arrow.core.getOrElse
import com.tangem.domain.common.util.getCardsCount
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.wallet.presentation.wallet.state.model.NOT_INITIALIZED_WALLET_INDEX
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import dagger.hilt.android.scopes.ViewModelScoped
import timber.log.Timber
import javax.inject.Inject

/**
 * Resolver that determines which update action will be performed
 *
 * @property getSelectedWalletSyncUseCase use case that returns selected wallet
 */
@ViewModelScoped
internal class WalletsUpdateActionResolver @Inject constructor(
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
) {

    fun resolve(wallets: List<UserWallet>, currentState: WalletScreenState): Action {
        val selectedWallet = getSelectedWalletSyncUseCase().getOrElse {
            /* Selected user wallet can be null after reset if remaining user wallets is locked */
            return Action.Unknown
        }

        val action = if (isFirstInitialization(currentState)) {
            createInitializeWalletsAction(wallets, selectedWallet)
        } else {
            getUpdateContentAction(currentState, wallets, selectedWallet)
        }

        Timber.i("Resolved action: $action")

        return action
    }

    private fun isFirstInitialization(state: WalletScreenState): Boolean {
        return state.selectedWalletIndex == NOT_INITIALIZED_WALLET_INDEX
    }

    private fun createInitializeWalletsAction(wallets: List<UserWallet>, selectedWallet: UserWallet): Action {
        return Action.InitializeWallets(
            selectedWalletIndex = wallets.indexOfWallet(selectedWallet.walletId),
            selectedWallet = selectedWallet,
            wallets = wallets,
        )
    }

    private fun getUpdateContentAction(
        state: WalletScreenState,
        wallets: List<UserWallet>,
        selectedWallet: UserWallet,
    ): Action {
        return when {
            isWalletsCountChanged(state, wallets) -> {
                getChangeWalletsListAction(state, wallets, selectedWallet)
            }
            isAnotherWalletSelected(state, selectedWallet) -> {
                Action.ReinitializeWallet(
                    prevWalletId = state.getPrevSelectedWallet().id,
                    selectedWallet = selectedWallet,
                )
            }
            isAnyWalletNameChanged(state, wallets) -> {
                getRenameWalletsAction(state, wallets)
            }
            else -> getUpdateSelectedWalletAction(state, wallets, selectedWallet)
        }
    }

    private fun isWalletsCountChanged(state: WalletScreenState, wallets: List<UserWallet>): Boolean {
        val prevWalletsSize = state.wallets.size
        val walletsSize = wallets.size

        return prevWalletsSize != walletsSize
    }

    private fun getChangeWalletsListAction(
        state: WalletScreenState,
        wallets: List<UserWallet>,
        selectedWallet: UserWallet,
    ): Action {
        val prevWalletsSize = state.wallets.size

        return when {
            prevWalletsSize > wallets.size -> {
                Action.DeleteWallet(
                    selectedWallet = selectedWallet,
                    selectedWalletIndex = wallets.indexOfWallet(id = selectedWallet.walletId),
                    deletedWalletId = state.wallets.getDeletedWalletId(wallets),
                )
            }
            prevWalletsSize < wallets.size -> {
                val newUserWallet = state.wallets.getAddedWallet(wallets)
                Action.AddWallet(
                    selectedWalletIndex = wallets.indexOfWallet(id = newUserWallet.walletId),
                    selectedWallet = newUserWallet,
                )
            }
            else -> error("Wallets list is not changed")
        }
    }

    private fun List<WalletState>.getDeletedWalletId(wallets: List<UserWallet>): UserWalletId {
        return this
            .map { it.walletCardState.id }
            .firstOrNull { !wallets.map(UserWallet::walletId).contains(it) }
            ?: error("Deleted wallet id is not found. Wallets contains all previous wallets ids")
    }

    private fun List<WalletState>.getAddedWallet(wallets: List<UserWallet>): UserWallet {
        return wallets
            .firstOrNull { wallet -> !this.map { it.walletCardState.id }.contains(wallet.walletId) }
            ?: error("Added wallet id is not found. Wallets contains all previous wallets ids")
    }

    private fun isAnotherWalletSelected(state: WalletScreenState, selectedWallet: UserWallet): Boolean {
        return state.getPrevSelectedWallet().id != selectedWallet.walletId
    }

    private fun isAnyWalletNameChanged(state: WalletScreenState, wallets: List<UserWallet>): Boolean {
        val prevWallets = state.wallets.map { it.walletCardState.id to it.walletCardState.title }
        val newWallets = wallets.map { it.walletId to it.name }

        val prevWalletsIds = prevWallets.map { it.first }
        val newWalletsIds = newWallets.map { it.first }

        val isAnyNameChanged = (newWallets - prevWallets.toSet()).isNotEmpty()

        return prevWalletsIds == newWalletsIds && isAnyNameChanged
    }

    private fun getRenameWalletsAction(state: WalletScreenState, wallets: List<UserWallet>): Action.RenameWallets {
        val prevWallets = state.wallets.map { it.walletCardState.id to it.walletCardState.title }
        val newWallets = wallets.map { it.walletId to it.name }

        val renamedWallets = newWallets - prevWallets.toSet()

        return Action.RenameWallets(
            renamedWallets = wallets.filter { wallet -> renamedWallets.any { it.first == wallet.walletId } },
        )
    }

    private fun getUpdateSelectedWalletAction(
        state: WalletScreenState,
        wallets: List<UserWallet>,
        selectedWallet: UserWallet,
    ): Action {
        return when {
            isSelectedWalletUnlocked(state, selectedWallet) -> {
                Action.UnlockWallet(
                    selectedWallet = selectedWallet,
                    unlockedWallets = wallets.filterNot(UserWallet::isLocked),
                )
            }
            isSelectedWalletCardsCountChanged(state, selectedWallet) -> {
                Action.UpdateWalletCardCount(selectedWallet)
            }
            else -> Action.Unknown
        }
    }

    private fun isSelectedWalletUnlocked(state: WalletScreenState, selectedWallet: UserWallet): Boolean {
        return state.isSelectedWalletLocked() && !selectedWallet.isLocked
    }

    private fun isSelectedWalletCardsCountChanged(state: WalletScreenState, selectedWallet: UserWallet): Boolean {
        val prevSelectedWallet = state.getPrevSelectedWallet()
        return prevSelectedWallet is WalletCardState.Content &&
            prevSelectedWallet.cardCount != selectedWallet.getCardsCount()
    }

    private fun WalletScreenState.isSelectedWalletLocked(): Boolean {
        val selectedWalletState = wallets.getOrNull(selectedWalletIndex) ?: error("Selected wallet is not found")
        return selectedWalletState is WalletState.MultiCurrency.Locked ||
            selectedWalletState is WalletState.SingleCurrency.Locked
    }

    private fun WalletScreenState.getPrevSelectedWallet(): WalletCardState {
        return wallets
            .map(WalletState::walletCardState)
            .getOrNull(selectedWalletIndex)
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

        data class InitializeWallets(
            val selectedWalletIndex: Int,
            val selectedWallet: UserWallet,
            val wallets: List<UserWallet>,
        ) : Action() {

            override fun toString(): String {
                return """
                    InitializeWallets(
                        selectedWalletIndex = $selectedWalletIndex,
                        selectedWallet = ${selectedWallet.walletId},
                        wallets = ${wallets.joinToString { it.walletId.toString() }}
                    )
                """.trimIndent()
            }
        }

        /**
         * Reinitialize selected wallet.
         * Uses when:
         * 1. user scanned a new card but wallets saving is turned off;
         * 2. user reset Twins.
         *
         * @property prevWalletId   previous selected wallet id
         * @property selectedWallet selected wallet
         */
        data class ReinitializeWallet(val prevWalletId: UserWalletId, val selectedWallet: UserWallet) : Action() {

            override fun toString(): String {
                return "ReinitializeWallet(prevWalletId = $prevWalletId, selectedWallet = ${selectedWallet.walletId})"
            }
        }

        /**
         * Rename wallets
         *
         * @property renamedWallets renamed wallets
         */
        data class RenameWallets(val renamedWallets: List<UserWallet>) : Action() {

            override fun toString(): String {
                return """
                    RenameWallets(
                        renamedWallets = ${renamedWallets.joinToString { it.walletId.toString() }}
                    )
                """.trimIndent()
            }
        }

        data class UnlockWallet(val selectedWallet: UserWallet, val unlockedWallets: List<UserWallet>) : Action() {

            override fun toString(): String {
                return """
                    UnlockWallet(
                        selectedWallet = ${selectedWallet.walletId},
                        unlockedWallets = ${unlockedWallets.joinToString { it.walletId.toString() }}
                    )
                """.trimIndent()
            }
        }

        data class DeleteWallet(
            val selectedWallet: UserWallet,
            val selectedWalletIndex: Int,
            val deletedWalletId: UserWalletId,
        ) : Action() {

            override fun toString(): String {
                return """
                    DeleteWallet(
                        selectedWallet = ${selectedWallet.walletId},
                        selectedWalletIndex = $selectedWalletIndex,
                        deletedWalletId = $deletedWalletId
                    )
                """.trimIndent()
            }
        }

        data class AddWallet(val selectedWalletIndex: Int, val selectedWallet: UserWallet) : Action() {

            override fun toString(): String {
                return """
                    AddWallet(
                        selectedWalletIndex = $selectedWalletIndex,
                        selectedWallet = ${selectedWallet.walletId}
                    )
                """.trimIndent()
            }
        }

        /**
         * Update wallet card count. Example, if user backed up wallet
         *
         * @property selectedWallet selected wallet
         */
        data class UpdateWalletCardCount(val selectedWallet: UserWallet) : Action() {

            override fun toString(): String {
                return "UpdateWalletCardCount(selectedWallet = ${selectedWallet.walletId})"
            }
        }

        data object Unknown : Action()
    }
}