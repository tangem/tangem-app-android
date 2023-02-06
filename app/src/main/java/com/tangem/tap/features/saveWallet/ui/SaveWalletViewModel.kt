package com.tangem.tap.features.saveWallet.ui

import androidx.lifecycle.ViewModel
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.tap.features.saveWallet.redux.SaveWalletAction
import com.tangem.tap.features.saveWallet.redux.SaveWalletState
import com.tangem.tap.features.saveWallet.ui.models.EnrollBiometricsDialog
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

    fun cancelOrClose() {
        store.dispatch(SaveWalletAction.Dismiss)
    }

    fun closeError() {
        store.dispatch(SaveWalletAction.CloseError)
    }

    override fun newState(state: SaveWalletState) {
        stateInternal.update { prevState ->
            prevState.copy(
                showProgress = state.isSaveInProgress,
                enrollBiometricsDialog = if (state.needEnrollBiometrics) createEnrollBiometricsDialog() else null,
                error = state.error
                    ?.takeUnless { it.silent }
                    ?.let { error ->
                        error.messageResId?.let { TextReference.Res(it) }
                            ?: TextReference.Str(error.customMessage)
                    },
            )
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

    private fun createEnrollBiometricsDialog() = EnrollBiometricsDialog(
        onEnroll = {
            store.dispatchOnMain(SaveWalletAction.EnrollBiometrics.Enroll)
        },
        onCancel = {
            store.dispatchOnMain(SaveWalletAction.EnrollBiometrics.Cancel)
        },
    )
}
