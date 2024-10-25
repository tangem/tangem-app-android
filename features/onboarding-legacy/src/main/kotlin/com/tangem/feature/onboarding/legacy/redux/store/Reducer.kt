package com.tangem.feature.onboarding.legacy.redux.store

import com.tangem.feature.onboarding.legacy.redux.OnboardingReduxState
import com.tangem.feature.onboarding.legacy.redux.products.note.redux.OnboardingNoteReducer
import org.rekotlin.Action

internal fun onboardingReducer(action: Action, state: OnboardingReduxState?): OnboardingReduxState {
    requireNotNull(state)

    return state.copy(
        onboardingNoteState = OnboardingNoteReducer.reduce(action, state),
    )
}