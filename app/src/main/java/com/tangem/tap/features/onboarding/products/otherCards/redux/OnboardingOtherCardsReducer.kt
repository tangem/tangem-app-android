package com.tangem.tap.features.onboarding.products.otherCards.redux

import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import org.rekotlin.Action

class OnboardingOtherCardsReducer {
    companion object {
        fun reduce(action: Action, state: AppState): OnboardingOtherCardsState = internalReduce(action, state)
    }
}

private fun internalReduce(action: Action, appState: AppState): OnboardingOtherCardsState {
    var state = appState.onboardingOtherCardsState

    when (action) {
        is GlobalAction.Onboarding.Start -> {
            state = OnboardingOtherCardsState()
        }
        is OnboardingOtherCardsAction.SetArtworkUrl -> {
            state = state.copy(cardArtwork = action.artwork)
        }
        is OnboardingOtherCardsAction.SetStepOfScreen -> {
            if (action.step != state.currentStep && state.steps.contains(action.step)) {
                state = state.copy(currentStep = action.step)
            }
        }
        is OnboardingOtherCardsAction.Confetti.Show -> {
            state = state.copy(showConfetti = true)
        }
        is OnboardingOtherCardsAction.Confetti.Hide -> {
            state = state.copy(showConfetti = false)
        }
    }

    return state
}