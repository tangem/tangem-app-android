package com.tangem.tap.features.welcome.ui

import androidx.lifecycle.ViewModel
import com.tangem.core.analytics.Analytics
import com.tangem.tap.common.analytics.events.SignIn
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.details.ui.cardsettings.TextReference
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
        initGlobalState()
    }

    fun unlockWallets() {
        Analytics.send(SignIn.ButtonBiometricSignIn())
        store.dispatch(WelcomeAction.ProceedWithBiometrics)
    }

    fun scanCard() {
        Analytics.send(SignIn.ButtonCardSignIn())
        store.dispatch(WelcomeAction.ProceedWithCard)
    }

    fun closeError() {
        store.dispatch(WelcomeAction.CloseError)
    }

    override fun newState(state: WelcomeState) {
        stateInternal.update { prevState ->
            prevState.copy(
                showUnlockWithBiometricsProgress = state.isUnlockWithBiometricsInProgress,
                showUnlockWithCardProgress = state.isUnlockWithCardInProgress,
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
            appState.skip { old, new -> old.welcomeState == new.welcomeState }
                .select { it.welcomeState }
        }
    }

    private fun initGlobalState() {
        store.dispatch(GlobalAction.RestoreAppCurrency)
        store.dispatch(GlobalAction.ExchangeManager.Init)
        store.dispatch(GlobalAction.FetchUserCountry)
    }
}
