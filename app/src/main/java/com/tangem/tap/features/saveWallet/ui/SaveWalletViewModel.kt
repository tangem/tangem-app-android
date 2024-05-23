package com.tangem.tap.features.saveWallet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.settings.SetSaveWalletScreenShownUseCase
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.tap.features.saveWallet.redux.SaveWalletAction
import com.tangem.tap.features.saveWallet.redux.SaveWalletState
import com.tangem.tap.features.saveWallet.ui.models.EnrollBiometricsDialog
import com.tangem.tap.store
import com.tangem.utils.coroutines.AppCoroutineDispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.rekotlin.StoreSubscriber
import javax.inject.Inject

@HiltViewModel
internal class SaveWalletViewModel @Inject constructor(
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val setSaveWalletScreenShownUseCase: SetSaveWalletScreenShownUseCase,
    dispatchers: AppCoroutineDispatcherProvider,
) : ViewModel(), StoreSubscriber<SaveWalletState> {

    private val stateInternal = MutableStateFlow(SaveWalletScreenState())
    val state: StateFlow<SaveWalletScreenState> = stateInternal

    init {
        subscribeToStoreChanges()

        viewModelScope.launch(dispatchers.main) {
            setSaveWalletScreenShownUseCase()
        }
    }

    fun saveWallet() {
        analyticsEventHandler.send(WalletScreenAnalyticsEvent.MainScreen.EnableBiometrics(AnalyticsParam.OnOffState.On))
        store.dispatch(SaveWalletAction.AllowToUseBiometrics)
    }

    fun cancelOrClose() {
        analyticsEventHandler.send(
            WalletScreenAnalyticsEvent.MainScreen.EnableBiometrics(AnalyticsParam.OnOffState.Off),
        )
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