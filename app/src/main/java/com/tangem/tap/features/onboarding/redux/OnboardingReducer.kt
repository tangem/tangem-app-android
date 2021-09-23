package com.tangem.tap.features.onboarding.redux

import com.tangem.tap.common.redux.AppState
import org.rekotlin.Action

class OnboardingReducer {
    companion object {
        fun reduce(action: Action, state: AppState): OnboardingState = internalReduce(action, state)
    }
}

private fun internalReduce(action: Action, appState: AppState): OnboardingState {

    if (action !is OnboardingAction) return appState.onboardingState

    var state = appState.onboardingState
    when (action) {
        is OnboardingAction.SetData -> {
            state = state.copy(onboardingData = action.data)
        }
    }

    return state
}