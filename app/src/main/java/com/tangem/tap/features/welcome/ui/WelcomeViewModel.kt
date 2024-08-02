package com.tangem.tap.features.welcome.ui

import android.os.Bundle
import androidx.lifecycle.*
import com.tangem.common.core.TangemError
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.bundle.unbundle
import com.tangem.common.routing.entity.SerializableIntent
import com.tangem.core.analytics.Analytics
import com.tangem.domain.wallets.legacy.UserWalletsListError
import com.tangem.tap.common.analytics.events.SignIn
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.tap.features.welcome.redux.WelcomeAction
import com.tangem.tap.features.welcome.redux.WelcomeState
import com.tangem.tap.features.welcome.ui.model.WarningModel
import com.tangem.tap.store
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.rekotlin.StoreSubscriber
import javax.inject.Inject

@HiltViewModel
internal class WelcomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel(),
    StoreSubscriber<WelcomeState>,
    DefaultLifecycleObserver {

    private val initialIntent: SerializableIntent? = savedStateHandle.get<Bundle>(AppRoute.Welcome.INITIAL_INTENT_KEY)
        ?.unbundle(SerializableIntent.serializer())

    private val stateInternal = MutableStateFlow(WelcomeScreenState())
    val state: StateFlow<WelcomeScreenState> = stateInternal

    override fun onCreate(owner: LifecycleOwner) {
        store.dispatch(WelcomeAction.SetCoroutineScope(viewModelScope))

        subscribeToStoreChanges()
        initGlobalState()

        val welcomeAction = if (initialIntent != null) {
            WelcomeAction.ProceedWithIntent(initialIntent.toIntent())
        } else {
            WelcomeAction.ProceedWithBiometrics()
        }

        store.dispatch(welcomeAction)
    }

    fun unlockWallets() {
        Analytics.send(SignIn.ButtonBiometricSignIn())
        store.dispatch(WelcomeAction.ProceedWithBiometrics())
    }

    fun scanCard() {
        Analytics.send(SignIn.ButtonCardSignIn())
        store.dispatch(WelcomeAction.ProceedWithCard)
    }

    fun closeError() {
        store.dispatch(WelcomeAction.CloseError)
    }

    // TODO: Refactor errors handling
    override fun newState(state: WelcomeState) {
        val warning = createWarningIfNeeded(state.error)

        stateInternal.update { prevState ->
            prevState.copy(
                showUnlockWithBiometricsProgress = state.isUnlockWithBiometricsInProgress,
                showUnlockWithCardProgress = state.isUnlockWithCardInProgress,
                warning = warning,
                error = state.error
                    ?.takeIf { !it.silent && warning == null }
                    ?.let { e ->
                        e.messageResId?.let { TextReference.Res(it) }
                            ?: TextReference.Str(e.customMessage)
                    },
            )
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        store.dispatch(WelcomeAction.ClearCoroutineScope)
        store.unsubscribe(this)
        super.onDestroy(owner)
    }

    private fun createWarningIfNeeded(error: TangemError?): WarningModel? {
        return when (error) {
            is UserWalletsListError.BiometricsAuthenticationLockout -> WarningModel.BiometricsLockoutWarning(
                isPermanent = error.isPermanent,
                onDismiss = this::dismissWarning,
            )
            is UserWalletsListError.AllKeysInvalidated,
            is UserWalletsListError.NoUserWalletSelected,
            -> WarningModel.KeyInvalidatedWarning(
                onDismiss = this::dismissWarning,
            )
            is UserWalletsListError.BiometricsAuthenticationDisabled -> WarningModel.BiometricsDisabledWarning(
                onDismiss = this::clearUserWallets,
            )
            else -> null
        }
    }

    private fun dismissWarning() {
        stateInternal.update { prevState ->
            prevState.copy(
                warning = null,
            )
        }
        closeError()
    }

    private fun clearUserWallets() {
        store.dispatch(WelcomeAction.ClearUserWallets)
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