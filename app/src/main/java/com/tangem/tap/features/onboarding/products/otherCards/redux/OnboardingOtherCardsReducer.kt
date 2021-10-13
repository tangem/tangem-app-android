package com.tangem.tap.features.onboarding.products.otherCards.redux

import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.onboarding.service.OnboardingOtherCardsService
import org.rekotlin.Action

class OnboardingOtherCardsReducer {
    companion object {
        fun reduce(action: Action, state: AppState): OnboardingOtherCardsState = internalReduce(action, state)
    }
}

private fun internalReduce(action: Action, appState: AppState): OnboardingOtherCardsState {
    var state = appState.onboardingOtherCardsState

    when (action) {
        is GlobalAction.Onboarding.Activate -> {
            if (action.onboardingService is OnboardingOtherCardsService) {
                val service = action.onboardingService
                state = state.copy(
                        onboardingService = service,
                        artworkBitmap = service.getArtwork().value?.artwork,
                        showConfetti = false
                )
            }
        }
        GlobalAction.Onboarding.Deactivate -> {
            state = OnboardingOtherCardsState()
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