package com.tangem.tap.features.onboarding.products.note.redux

import com.tangem.common.CompletionResult
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.postUi
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.onboarding.service.balanceIsToppedUp
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware

class OnboardingNoteMiddleware {
    companion object {
        val handler = onboardingNoteMiddleware
    }
}

private val onboardingNoteMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            handleNoteAction(action)
            next(action)
        }
    }
}

private fun handleNoteAction(action: Action) {
    if (action !is OnboardingNoteAction) return
    val service = store.state.onboardingNoteState.onboardingService ?: return
    val noteState = store.state.onboardingNoteState

    when (action) {
        is OnboardingNoteAction.DetermineStepOfScreen -> {
            val step = when {
                service.walletManager == null -> OnboardingNoteStep.CreateWallet
                service.balanceIsToppedUp() -> OnboardingNoteStep.Done
                else -> OnboardingNoteStep.TopUpWallet
            }
            store.dispatch((OnboardingNoteAction.SetStepOfScreen(step)))
        }
        is OnboardingNoteAction.SetStepOfScreen -> {
            if (action.step == OnboardingNoteStep.Done) {
                service.activationFinished()
                postUi(500) { store.dispatch(OnboardingNoteAction.Confetti.Show) }
            }
        }
        is OnboardingNoteAction.CreateWallet -> {
            scope.launch {
                val result = tangemSdkManager.createProductWallet(service.scanResponse.card.cardId)
                withMainContext {
                    when (result) {
                        is CompletionResult.Success -> {
                            val updatedResponse = service.scanResponse.copy(card = result.data)
                            service.scanResponse = updatedResponse
                            service.activationStarted()

                            store.dispatch(OnboardingNoteAction.SetStepOfScreen(OnboardingNoteStep.TopUpWallet))
                            store.dispatch(OnboardingNoteAction.Balance.Update)
                        }
                        is CompletionResult.Failure -> {
//                            do nothing
                        }
                    }
                }
            }
        }
        is OnboardingNoteAction.Balance.Update -> {
            val loadingBalance = service.getBalance().copy(
                    state = ProgressState.Loading,
                    error = null,
                    criticalError = null
            )
            store.dispatch(OnboardingNoteAction.Balance.Set(loadingBalance))

            scope.launch {
                val loadedBalance = service.updateBalance()
                if (loadedBalance.criticalError != null) {
                    store.dispatchErrorNotification(loadedBalance.criticalError)
                }
                withMainContext { store.dispatch(OnboardingNoteAction.Balance.Set(loadedBalance)) }
            }
        }
        is OnboardingNoteAction.Balance.Set -> {
            if (service.balanceIsToppedUp()) {
                store.dispatch(OnboardingNoteAction.SetStepOfScreen(OnboardingNoteStep.Done))
            }
        }
        is OnboardingNoteAction.ShowAddressInfoDialog -> {
            val addressData = noteState.onboardingService?.getAddressData() ?: return
            val addressWasCopied = noteState.resources.strings.addressWasCopied
            val appDialog = AppDialog.AddressInfoDialog(
                    addressData,
                    onCopyAddress = { store.dispatchToastNotification(addressWasCopied) },
                    onExploreAddress = { store.dispatchOpenUrl(addressData.exploreUrl) }
            )
            store.dispatchDialogShow(appDialog)
        }
        is OnboardingNoteAction.TopUp -> {
            val topUpUrl = noteState.onboardingService?.getToUpUrl() ?: return
            store.dispatchOpenUrl(topUpUrl)
        }
        OnboardingNoteAction.Done -> {
            store.dispatch(GlobalAction.Onboarding.Deactivate)
            scope.launch {
                store.state.globalState.tapWalletManager.onCardScanned(service.scanResponse)
                store.dispatch(NavigationAction.NavigateTo(AppScreen.Wallet))
            }
        }
    }
}