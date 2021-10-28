package com.tangem.tap.features.onboarding.products.note.redux

import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import org.rekotlin.Action

class OnboardingNoteReducer {
    companion object {
        fun reduce(action: Action, state: AppState): OnboardingNoteState = internalReduce(action, state)
    }
}

private fun internalReduce(action: Action, appState: AppState): OnboardingNoteState {
    var state = appState.onboardingNoteState

    when (action) {
        is GlobalAction.Onboarding.Start -> {
            state = OnboardingNoteState()
        }
        is OnboardingNoteAction.SetArtwork -> {
            state = state.copy(cardArtwork = action.artwork)
        }
        is OnboardingNoteAction.SetWalletManager -> {
            state = state.copy(walletManager = action.walletManager)
        }
        is OnboardingNoteAction.Balance.Set -> {
            state = state.copy(walletBalance = action.balance)
        }
        is OnboardingNoteAction.Balance.SetNonCriticalError -> {
            state = state.copy(balanceNonCriticalError = action.error)
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