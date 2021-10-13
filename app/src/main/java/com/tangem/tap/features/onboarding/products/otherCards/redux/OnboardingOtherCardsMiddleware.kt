package com.tangem.tap.features.onboarding.products.otherCards.redux

import com.tangem.common.CompletionResult
import com.tangem.tap.common.extensions.withMainContext
import com.tangem.tap.common.postUi
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.DELAY_SDK_DIALOG_CLOSE
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware

class OnboardingOtherCardsMiddleware {
    companion object {
        val handler = onboardingOtherCardsMiddleware
    }
}

private val onboardingOtherCardsMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            handleOtherCardsAction(action)
            next(action)
        }
    }
}

private fun handleOtherCardsAction(action: Action) {
    if (action !is OnboardingOtherCardsAction) return
    val service = store.state.onboardingOtherCardsState.onboardingService ?: return
    val noteState = store.state.onboardingOtherCardsState

    when (action) {
        is OnboardingOtherCardsAction.DetermineStepOfScreen -> {
            val step = when {
                service.walletManager == null -> OnboardingOtherCardsStep.CreateWallet
                else -> OnboardingOtherCardsStep.Done
            }
            store.dispatch((OnboardingOtherCardsAction.SetStepOfScreen(step)))
        }
        is OnboardingOtherCardsAction.SetStepOfScreen -> {
            if (action.step == OnboardingOtherCardsStep.Done) {
                service.activationFinished()
                postUi(500) { store.dispatch(OnboardingOtherCardsAction.Confetti.Show) }
            }
        }
        is OnboardingOtherCardsAction.CreateWallet -> {
            scope.launch {
                val result = tangemSdkManager.createProductWallet(service.scanResponse)
                withMainContext {
                    when (result) {
                        is CompletionResult.Success -> {
                            val updatedResponse = service.scanResponse.copy(card = result.data)
                            service.scanResponse = updatedResponse
                            service.activationStarted()

                            delay(DELAY_SDK_DIALOG_CLOSE)
                            store.dispatch(OnboardingOtherCardsAction.SetStepOfScreen(OnboardingOtherCardsStep.Done))
                        }
                        is CompletionResult.Failure -> {
//                            do nothing
                        }
                    }
                }
            }
        }
        OnboardingOtherCardsAction.Done -> {
            store.dispatch(GlobalAction.Onboarding.Deactivate)
            scope.launch {
                store.state.globalState.tapWalletManager.onCardScanned(service.scanResponse)
                store.dispatch(NavigationAction.NavigateTo(AppScreen.Wallet))
            }
        }
    }
}