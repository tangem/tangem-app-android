package com.tangem.tap.features.onboarding.products.note.redux

import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.onboarding.redux.OnboardingAction
import com.tangem.tap.features.onboarding.redux.OnboardingNoteStep
import org.rekotlin.Action

class OnboardingNoteReducer {
    companion object {
        fun reduce(action: Action, state: AppState): OnboardingNoteState = internalReduce(action, state)
    }
}

private fun internalReduce(action: Action, appState: AppState): OnboardingNoteState {
    var state = appState.onboardingNoteState

    when (action) {
        is OnboardingAction.SetInitialStepOfScreen -> {
            (action.step as? OnboardingNoteStep)?.let {
                if (it != state.currentStep && state.steps.contains(it)) {
                    state = state.copy(currentStep = it)
                }
            }
        }
        OnboardingAction.SwitchToNextStep -> {
            val progress = state.progress
            if (progress >= 0 && progress != state.steps.size - 1) {
                state = state.copy(currentStep = state.steps[progress + 1])
            }
        }
        is OnboardingNoteAction.SetWalletManager -> {
            state = state.copy(walletManager = action.walletManager)
        }

    }

    return state
}