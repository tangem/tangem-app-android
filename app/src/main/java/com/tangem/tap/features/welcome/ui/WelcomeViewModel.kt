package com.tangem.tap.features.welcome.ui

import androidx.lifecycle.ViewModel
import com.tangem.tap.features.welcome.redux.WelcomeAction
import com.tangem.tap.features.welcome.redux.WelcomeState
import com.tangem.tap.store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.rekotlin.StoreSubscriber

internal class WelcomeViewModel : ViewModel(), StoreSubscriber<WelcomeState> {
    private val stateInternal = MutableStateFlow(WelcomeScreenState())
    val state: StateFlow<WelcomeScreenState> = stateInternal

    init {
        subscribeToStoreChanges()
    }

    fun unlockWallets() {
        store.dispatch(WelcomeAction.ProceedWithBiometrics)
    }

    fun scanCard() {
        store.dispatch(WelcomeAction.ProceedWithCard)
    }

    fun closeError() {
        store.dispatch(WelcomeAction.CloseError)
    }

    override fun newState(state: WelcomeState) {
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
            appState.skip { old, new -> old.welcomeState == new.welcomeState }
                .select { it.welcomeState }
        }
    }
}