package com.tangem.tap.features.onboarding.products.otherCards.redux

import com.tangem.common.CompletionResult
import com.tangem.tap.common.extensions.withMainContext
import com.tangem.tap.common.postUi
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.DELAY_SDK_DIALOG_CLOSE
import com.tangem.tap.domain.extensions.hasWallets
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.DispatchFunction
import org.rekotlin.Middleware

class OnboardingOtherCardsMiddleware {
    companion object {
        val handler = onboardingOtherCardsMiddleware
    }
}

private val onboardingOtherCardsMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            handleOtherCardsAction(action, dispatch)
            next(action)
        }
    }
}

private fun handleOtherCardsAction(action: Action, dispatch: DispatchFunction) {
    if (action !is OnboardingOtherCardsAction) return
    val globalState = store.state.globalState
    val onboardingManager = globalState.onboardingManager ?: return

    val card = onboardingManager.scanResponse.card

    when (action) {
        is OnboardingOtherCardsAction.LoadCardArtwork -> {
            scope.launch {
                val artwork = onboardingManager.loadArtwork()
                withMainContext { store.dispatch(OnboardingOtherCardsAction.SetArtwork(artwork)) }
            }
        }
        is OnboardingOtherCardsAction.DetermineStepOfScreen -> {
            val step = when {
                !card.hasWallets() -> OnboardingOtherCardsStep.CreateWallet
                else -> OnboardingOtherCardsStep.Done
            }
            store.dispatch((OnboardingOtherCardsAction.SetStepOfScreen(step)))
        }
        is OnboardingOtherCardsAction.SetStepOfScreen -> {
            if (action.step == OnboardingOtherCardsStep.Done) {
                onboardingManager.activationFinished(card.cardId)
                postUi(500) { store.dispatch(OnboardingOtherCardsAction.Confetti.Show) }
            }
        }
        is OnboardingOtherCardsAction.CreateWallet -> {
            scope.launch {
                val result = tangemSdkManager.createProductWallet(onboardingManager.scanResponse)
                withMainContext {
                    when (result) {
                        is CompletionResult.Success -> {
                            val updatedResponse = onboardingManager.scanResponse.copy(card = result.data)
                            onboardingManager.scanResponse = updatedResponse
                            onboardingManager.activationStarted(updatedResponse.card.cardId)

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
            store.dispatch(GlobalAction.Onboarding.Stop)
            scope.launch {
                globalState.tapWalletManager.onCardScanned(onboardingManager.scanResponse)
                store.dispatch(NavigationAction.NavigateTo(AppScreen.Wallet))
            }
        }
    }
}