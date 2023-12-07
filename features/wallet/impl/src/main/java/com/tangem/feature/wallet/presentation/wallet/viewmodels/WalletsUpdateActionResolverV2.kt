package com.tangem.feature.wallet.presentation.wallet.viewmodels

import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.feature.wallet.presentation.wallet.domain.getCardsCount
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state2.model.NOT_INITIALIZED_WALLET_INDEX
import com.tangem.feature.wallet.presentation.wallet.state2.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state2.model.WalletState
import dagger.hilt.android.scopes.ViewModelScoped
import timber.log.Timber
import javax.inject.Inject

/**
 * Resolver that determines which update action will be performed
 *
 * @property getSelectedWalletSyncUseCase use case that returns selected wallet
 */
@ViewModelScoped
internal class WalletsUpdateActionResolverV2 @Inject constructor(
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
) {

    private var isInitialized: Boolean = false
    private var canSaveWallets: Boolean = false

    fun resolve(wallets: List<UserWallet>, currentState: WalletScreenState, canSaveWallets: Boolean): Action {
        val selectedWallet = wallets.getSelectedWallet() ?: return Action.Unknown

        val action = when {
            isFirstInitialization(currentState) -> {
                createInitializeWalletsAction(wallets, selectedWallet, canSaveWallets)
            }
            isReinitialization(canSaveWallets) -> {
                this.canSaveWallets = canSaveWallets
                Action.ReinitializeWallets(selectedWallet = selectedWallet)
            }
            else -> getUpdateContentAction(currentState, wallets, selectedWallet)
        }

        Timber.d("Resolved action: $action")

        return action
    }

    private fun List<UserWallet>.getSelectedWallet(): UserWallet? {
        return when {
            isEmpty() -> null
            size == 1 -> first()
            else -> getSelectedWalletSyncUseCase().fold(ifLeft = { null }, ifRight = { it })
        }
    }

    private fun isFirstInitialization(state: WalletScreenState): Boolean {
        return state.selectedWalletIndex == NOT_INITIALIZED_WALLET_INDEX
    }

    private fun createInitializeWalletsAction(
        wallets: List<UserWallet>,
        selectedWallet: UserWallet,
        canSaveWallets: Boolean,
    ): Action {
        this.isInitialized = true
        this.canSaveWallets = canSaveWallets

        return Action.InitializeWallets(
            selectedWalletIndex = wallets.indexOfWallet(selectedWallet.walletId),
            selectedWallet = selectedWallet,
            wallets = wallets,
        )
    }

    private fun isReinitialization(canSaveWallets: Boolean): Boolean {
        return isInitialized && this.canSaveWallets != canSaveWallets
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

    private fun getUpdateSelectedWalletAction(
        state: WalletScreenState,
        wallets: List<UserWallet>,
        selectedWallet: UserWallet,
    ): Action {
        return when {
            isSelectedWalletNameChanged(state, selectedWallet) -> {
                Action.UpdateWalletName(selectedWalletId = selectedWallet.walletId, name = selectedWallet.name)
            }
            isSelectedWalletUnlocked(state, selectedWallet) -> {
                Action.UnlockWallet(
                    selectedWallet = selectedWallet,
                    unlockedWallets = wallets.filterNot(UserWallet::isLocked),
                )
            }
            isSelectedWalletCardsCountChanged(state, selectedWallet) -> Action.UpdateWalletCardCount(selectedWallet)
            else -> Action.Unknown
        }
    }

    private fun isSelectedWalletNameChanged(state: WalletScreenState, selectedWallet: UserWallet): Boolean {
        return state.getPrevSelectedWallet().title != selectedWallet.name
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
                    Initialize(
                        selectedWalletIndex=$selectedWalletIndex,
                        selectedWallet=${selectedWallet.walletId},
                        wallets=${wallets.joinToString { it.walletId.toString() }}
                    )
                """.trimIndent()
            }
        }

        /**
         * Reinitialize wallets. Example, if user turned on wallets saving
         *
         * @property selectedWallet selected wallet
         */
        data class ReinitializeWallets(val selectedWallet: UserWallet) : Action() {

            override fun toString(): String {
                return "Reinitialize(selectedWallet=${selectedWallet.walletId})"
            }
        }

        /**
         * Reinitialize selected wallet. Example, scanning a new card if wallets saving is turned off
         *
         * @property prevWalletId   previous selected wallet id
         * @property selectedWallet selected wallet
         */
        data class ReinitializeWallet(val prevWalletId: UserWalletId, val selectedWallet: UserWallet) : Action() {

            override fun toString(): String {
                return "ReinitializeWallet(prevWalletId=$prevWalletId, selectedWallet=${selectedWallet.walletId})"
            }
        }

        data class UpdateWalletName(val selectedWalletId: UserWalletId, val name: String) : Action() {

            override fun toString(): String {
                return "UpdateWalletName(selectedWalletId=$selectedWalletId, name=$name)"
            }
        }

        data class UnlockWallet(val selectedWallet: UserWallet, val unlockedWallets: List<UserWallet>) : Action() {

            override fun toString(): String {
                return """
                    UnlockWallet(
                        selectedWallet=${selectedWallet.walletId},
                        unlockedWallets=${unlockedWallets.joinToString { it.walletId.toString() }}
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
                        selectedWallet=${selectedWallet.walletId},
                        selectedWalletIndex=$selectedWalletIndex,
                        deletedWalletId=$deletedWalletId
                    )
                """.trimIndent()
            }
        }

        data class AddWallet(val selectedWalletIndex: Int, val selectedWallet: UserWallet) : Action() {

            override fun toString(): String {
                return "AddWallet(selectedWalletIndex=$selectedWalletIndex, selectedWallet=${selectedWallet.walletId})"
            }
        }

        /**
         * Update wallet card count. Example, if user backed up wallet
         *
         * @property selectedWallet selected wallet
         */
        data class UpdateWalletCardCount(val selectedWallet: UserWallet) : Action() {

            override fun toString(): String {
                return "UpdateWalletCardCount(selectedWallet=${selectedWallet.walletId})"
            }
        }

        object Unknown : Action()
    }
}