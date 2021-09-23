package com.tangem.tap.features.onboarding.products.note.redux

import com.tangem.common.CompletionResult
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.onboarding.redux.OnboardingAction
import com.tangem.tap.features.onboarding.redux.OnboardingNoteStep
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import kotlinx.coroutines.launch
import org.rekotlin.Action
import org.rekotlin.Middleware

class OnboardingNoteMiddleware {
    companion object {
        private const val CARD_SHOP_URI =
                "https://shop.tangem.com/?afmc=1i&utm_campaign=1i&utm_source=leaddyno&utm_medium=affiliate"
        val handler = onboardingNoteMiddleware
    }
}

private val onboardingNoteMiddleware: Middleware<AppState> = { dispatch, state ->
    { next ->
        { action ->
            handleMainOnboardingAction(action, ::handleNoteAction)
            next(action)
        }
    }
}

private fun handleMainOnboardingAction(action: Action, isNotMainAction: (Action) -> Unit) {
    val action = action as? OnboardingAction ?: isNotMainAction(action)

    when (action) {
    }
}

private fun handleNoteAction(action: Action) {
    val onboardingState = store.state.onboardingState
    val noteState = store.state.onboardingNoteState

    when (action) {
        is OnboardingNoteAction.SetWalletManager -> {
        }
        is OnboardingNoteAction.Init -> {
            val step = if (noteState.walletManager == null) {
                OnboardingNoteStep.CreateWallet
            } else {
                OnboardingNoteStep.TopUpWallet
            }
            store.dispatch(OnboardingAction.SetInitialStepOfScreen(step))
        }
        is OnboardingNoteAction.CreateWallet -> {
            scope.launch {
                val scanNoteResponse = onboardingState.onboardingData?.scanNoteResponse
                val result = tangemSdkManager.createWallet(scanNoteResponse?.card?.cardId)
                when (result) {
                    is CompletionResult.Success -> {
//                                val scanNoteResponse = scanNoteResponse!!.copy(card = result.data)
//                                store.dispatch(OnboardingAction.SetScanNoteResponse(scanNoteResponse))
                    }
                    is CompletionResult.Failure -> {
                        store.dispatch(OnboardingAction.Error("Failed to create wallet"))
                    }
                }
            }
        }
        is OnboardingNoteAction.TopUp -> {

        }
    }
}