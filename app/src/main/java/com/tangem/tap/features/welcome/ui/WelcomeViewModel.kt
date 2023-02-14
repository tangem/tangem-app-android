package com.tangem.tap.features.welcome.ui

import androidx.lifecycle.ViewModel
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.core.analytics.Analytics
import com.tangem.tap.common.analytics.events.SignIn
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.tap.features.welcome.redux.WelcomeAction
import com.tangem.tap.features.welcome.redux.WelcomeState
import com.tangem.tap.features.welcome.ui.model.BiometricsLockoutDialog
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
// [REDACTED_TODO_COMMENT]
    override fun newState(state: WelcomeState) {
        val biometricsLockoutDialog = createBiometricLockoutDialogIfNeeded(state.error)

        stateInternal.update { prevState ->
            prevState.copy(
                showUnlockWithBiometricsProgress = state.isUnlockWithBiometricsInProgress,
                showUnlockWithCardProgress = state.isUnlockWithCardInProgress,
                biometricsLockoutDialog = biometricsLockoutDialog,
                error = state.error
                    ?.takeIf { !it.silent && biometricsLockoutDialog == null }
                    ?.let { e ->
                        e.messageResId?.let { TextReference.Res(it) }
                            ?: TextReference.Str(e.customMessage)
                    },
            )
        }
    }

    override fun onCleared() {
        store.unsubscribe(this)
    }

    private fun createBiometricLockoutDialogIfNeeded(error: TangemError?): BiometricsLockoutDialog? {
        return when (error) {
            is TangemSdkError.BiometricsAuthenticationLockout -> BiometricsLockoutDialog(
                isPermanent = false,
                onDismiss = this::dismissDialog,
            )
            is TangemSdkError.BiometricsAuthenticationPermanentLockout -> BiometricsLockoutDialog(
                isPermanent = true,
                onDismiss = this::dismissDialog,
            )
            else -> null
        }
    }

    private fun dismissDialog() {
        stateInternal.update { prevState ->
            prevState.copy(
                biometricsLockoutDialog = null,
            )
        }
        closeError()
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
