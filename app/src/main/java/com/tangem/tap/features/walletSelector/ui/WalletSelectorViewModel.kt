package com.tangem.tap.features.walletSelector.ui

import androidx.lifecycle.ViewModel
import com.tangem.tap.features.walletSelector.redux.WalletSelectorAction
import com.tangem.tap.features.walletSelector.redux.WalletSelectorState
import com.tangem.tap.features.walletSelector.ui.model.RenameWalletDialog
import com.tangem.tap.store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.rekotlin.StoreSubscriber

internal class WalletSelectorViewModel : ViewModel(), StoreSubscriber<WalletSelectorState> {
    private val stateInternal = MutableStateFlow(WalletSelectorScreenState())
    val state: StateFlow<WalletSelectorScreenState> = stateInternal

    init {
        subscribeToStoreChanges()
        bootstrapWalletListChanges()
        bootstrapWalletsStoresChanges()
    }

    fun unlock() {
        store.dispatch(WalletSelectorAction.UnlockWithBiometry)
    }

    fun addWallet() {
        store.dispatch(WalletSelectorAction.AddWallet)
    }

    fun walletClicked(walletId: String) = with(state.value) {
        when {
            isLocked -> Unit
            editingWalletsIds.isNotEmpty() && !editingWalletsIds.contains(walletId) -> {
                editWallet(walletId)
            }
            editingWalletsIds.isNotEmpty() && editingWalletsIds.contains(walletId) -> {
                cancelWalletEditing(walletId)
            }
            selectedWalletId != walletId -> {
                store.dispatch(WalletSelectorAction.SelectWallet(walletId))
            }
        }
    }

    fun walletLongClicked(walletId: String) = with(state.value) {
        if (!isLocked && editingWalletsIds.isEmpty()) {
            editWallet(walletId)
        }
    }

    fun cancelWalletsEditing() {
        stateInternal.update { prevState ->
            prevState.copy(
                editingWalletsIds = emptyList(),
            )
        }
    }

    fun renameWallet() = with(state.value) {
        if (editingWalletsIds.isNotEmpty() && renameWalletDialog == null) {
            val editedWalletId = editingWalletsIds.first()
            val editedWallet = wallets.find { it.id == editedWalletId }

            if (editedWallet != null) {
                val dialog = RenameWalletDialog(
                    currentName = editedWallet.name,
                    onApply = { newName ->
                        store.dispatch(WalletSelectorAction.RenameWallet(editedWalletId, newName))
                        stateInternal.update { prevState ->
                            prevState.copy(
                                renameWalletDialog = null,
                                editingWalletsIds = emptyList(),
                            )
                        }
                    },
                    onCancel = {
                        stateInternal.update { prevState ->
                            prevState.copy(
                                renameWalletDialog = null,
                            )
                        }
                    },
                )

                stateInternal.update { prevState ->
                    prevState.copy(
                        renameWalletDialog = dialog,
                    )
                }
            }
        }
    }

    fun deleteWallets() = with(state.value) {
        if (editingWalletsIds.isNotEmpty()) {
            store.dispatch(WalletSelectorAction.RemoveWallets(walletIdsToRemove = editingWalletsIds))
        }
    }

    fun closeError() {
        store.dispatch(WalletSelectorAction.CloseError)
    }

    override fun newState(state: WalletSelectorState) {
        // TODO: Update UI state
    }

    override fun onCleared() {
        store.unsubscribe(this)
    }

    private fun editWallet(walletId: String) {
        stateInternal.update { prevState ->
            prevState.copy(
                editingWalletsIds = prevState.editingWalletsIds + walletId,
            )
        }
    }

    private fun cancelWalletEditing(walletId: String) {
        stateInternal.update { prevState ->
            prevState.copy(
                editingWalletsIds = prevState.editingWalletsIds - walletId,
            )
        }
    }

    private fun subscribeToStoreChanges() {
        store.subscribe(this) { appState ->
            appState.skip { old, new -> old.walletSelectorState == new.walletSelectorState }
                .select { it.walletSelectorState }
        }
    }

    private fun bootstrapWalletListChanges() {
        // TODO
    }

    private fun bootstrapWalletsStoresChanges() {
        // TODO
    }
}