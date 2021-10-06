package com.tangem.tap.features.onboarding.products.note.redux

import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.onboarding.service.OnboardingNoteService
import org.rekotlin.Action

class OnboardingNoteReducer {
    companion object {
        fun reduce(action: Action, state: AppState): OnboardingNoteState = internalReduce(action, state)
    }
}

private fun internalReduce(action: Action, appState: AppState): OnboardingNoteState {
    var state = appState.onboardingNoteState

    when (action) {
        is GlobalAction.Onboarding.Activate -> {
            if (action.onboardingService is OnboardingNoteService) {
                val service = action.onboardingService
                val balance = service.getBalance()
                state = state.copy(
                        onboardingService = service,
                        artworkBitmap = service.getArtwork().value?.artwork,
                        balanceValue = balance.value,
                        balanceCurrency = balance.currency,
                        balanceState = balance.state,
                        amountToCreateAccount = balance.amountToCreateAccount,
                        showConfetti = false
                )
            }
        }
        GlobalAction.Onboarding.Deactivate -> {
            state = OnboardingNoteState()
        }
        is OnboardingNoteAction.SetResources -> {
            state = state.copy(resources = action.resources)
        }
        is OnboardingNoteAction.Balance.Set -> {
            state = state.copy(
                    balanceValue = action.balance.value,
                    balanceCurrency = action.balance.currency,
                    balanceState = action.balance.state,
                    amountToCreateAccount = action.balance.amountToCreateAccount,
            )
        }
        is OnboardingNoteAction.SetStepOfScreen -> {
            if (action.step != state.currentStep && state.steps.contains(action.step)) {
                state = state.copy(currentStep = action.step)
            }
        }
        is OnboardingNoteAction.Confetti.Show -> {
            state = state.copy(showConfetti = true)
        }
        is OnboardingNoteAction.Confetti.Hide -> {
            state = state.copy(showConfetti = false)
        }
    }

    return state
}