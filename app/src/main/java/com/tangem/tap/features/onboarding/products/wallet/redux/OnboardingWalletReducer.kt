package com.tangem.tap.features.onboarding.products.wallet.redux

import com.tangem.tap.common.redux.AppState
import org.rekotlin.Action

class OnboardingWalletReducer {
    companion object {
        fun reduce(action: Action, state: AppState): OnboardingWalletState = internalReduce(action, state)
    }
}

private fun internalReduce(action: Action, appState: AppState): OnboardingWalletState {
    var state = appState.onboardingWalletState

    when (action) {

    }

    return state
}