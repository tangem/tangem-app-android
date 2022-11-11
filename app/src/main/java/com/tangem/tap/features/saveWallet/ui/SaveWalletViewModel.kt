package com.tangem.tap.features.saveWallet.ui

import androidx.lifecycle.ViewModel
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.features.saveWallet.redux.SaveWalletAction
import com.tangem.tap.features.saveWallet.redux.SaveWalletState
import com.tangem.tap.store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.rekotlin.StoreSubscriber

internal class SaveWalletViewModel : ViewModel(), StoreSubscriber<SaveWalletState> {
    private val stateInternal = MutableStateFlow(SaveWalletScreenState())
    val state: StateFlow<SaveWalletScreenState> = stateInternal

    init {
        subscribeToStoreChanges()
        store.dispatchOnMain(SaveWalletAction.SaveWalletWasShown)
    }

    fun saveWallet() {
        store.dispatch(SaveWalletAction.Save)
    }

    fun dismiss() {
        store.dispatch(SaveWalletAction.Dismiss)
    }

    fun closeError() {
        store.dispatch(SaveWalletAction.CloseError)
    }

    override fun newState(state: SaveWalletState) {
        stateInternal.update { prevState ->
            // TODO: Update screen state
            prevState
        }
    }

    override fun onCleared() {
        store.unsubscribe(this)
    }

    private fun subscribeToStoreChanges() {
        store.subscribe(this) { appState ->
            appState.skip { old, new -> old.saveWalletState == new.saveWalletState }
                .select { it.saveWalletState }
        }
    }
}