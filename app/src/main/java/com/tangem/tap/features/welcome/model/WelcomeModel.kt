package com.tangem.tap.features.welcome.model

import com.tangem.common.core.TangemError
import com.tangem.core.analytics.Analytics
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.finisher.AppFinisher
import com.tangem.domain.wallets.legacy.UserWalletsListError
import com.tangem.tap.common.analytics.events.SignIn
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.tap.features.welcome.component.WelcomeComponent
import com.tangem.tap.features.welcome.redux.WelcomeAction
import com.tangem.tap.features.welcome.redux.WelcomeState
import com.tangem.tap.features.welcome.ui.WelcomeScreenState
import com.tangem.tap.features.welcome.ui.model.WarningModel
import com.tangem.tap.store
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.rekotlin.StoreSubscriber
import javax.inject.Inject

// FIXME: Remove redux: [REDACTED_JIRA]
@ModelScoped
internal class WelcomeModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val appFinisher: AppFinisher,
    paramsContainer: ParamsContainer,
) : Model(), StoreSubscriber<WelcomeState> {

    private val params: WelcomeComponent.Params = paramsContainer.require()
    private val initialState: WelcomeScreenState = WelcomeScreenState(
        onPopBack = appFinisher::finish,
        onUnlockClick = this::unlockWallets,
        onScanCardClick = this::scanCard,
        onCloseError = this::closeError,
    )

    val state: MutableStateFlow<WelcomeScreenState> = MutableStateFlow(initialState)

    init {
        subscribeToStoreChanges()
        initGlobalState()

        val welcomeAction = if (params.intent != null) {
            WelcomeAction.ProceedWithIntent(params.intent.toIntent())
        } else {
            WelcomeAction.ProceedWithBiometrics()
        }

        store.dispatch(welcomeAction)
    }

    private fun unlockWallets() {
        Analytics.send(SignIn.ButtonBiometricSignIn())
        store.dispatch(WelcomeAction.ProceedWithBiometrics())
    }

    private fun scanCard() {
        Analytics.send(SignIn.ButtonCardSignIn())
        store.dispatch(WelcomeAction.ProceedWithCard)
    }

    private fun closeError() {
        store.dispatch(WelcomeAction.CloseError)
    }

    override fun newState(state: WelcomeState) {
        val warning = createWarningIfNeeded(state.error)

        this.state.update { prevState ->
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

    override fun onDestroy() {
        store.unsubscribe(subscriber = this)

        super.onDestroy()
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
        state.update { prevState ->
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
    }
}