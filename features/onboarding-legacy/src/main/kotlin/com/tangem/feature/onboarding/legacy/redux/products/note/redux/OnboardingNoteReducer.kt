package com.tangem.feature.onboarding.legacy.redux.products.note.redux

import com.tangem.feature.onboarding.legacy.redux.OnboardingGlobalAction
import com.tangem.feature.onboarding.legacy.redux.OnboardingReduxState
import org.rekotlin.Action

internal object OnboardingNoteReducer {
    fun reduce(action: Action, state: OnboardingReduxState): OnboardingNoteState = internalReduce(action, state)
}

private fun internalReduce(action: Action, state: OnboardingReduxState): OnboardingNoteState {
    var stateResult = state.onboardingNoteState

    when (action) {
        is OnboardingGlobalAction.Start -> {
            stateResult = OnboardingNoteState(scanResponse = action.scanResponse)
        }
        is OnboardingNoteAction.SetArtworkUrl -> {
            stateResult = stateResult.copy(cardArtworkUrl = action.artworkUrl)
        }
        is OnboardingNoteAction.SetWalletManager -> {
            stateResult = stateResult.copy(walletManager = action.walletManager)
        }
        is OnboardingNoteAction.Balance.Set -> {
            stateResult = stateResult.copy(walletBalance = action.balance)
        }
        is OnboardingNoteAction.Balance.SetCriticalError -> {
            stateResult = stateResult.copy(balanceCriticalError = action.error)
        }
        is OnboardingNoteAction.Balance.SetNonCriticalError -> {
            stateResult = stateResult.copy(balanceNonCriticalError = action.error)
        }
        is OnboardingNoteAction.SetStepOfScreen -> {
            if (action.step != stateResult.currentStep && stateResult.steps.contains(action.step)) {
                stateResult = stateResult.copy(currentStep = action.step)
            }
        }
        is OnboardingNoteAction.Confetti.Show -> {
            stateResult = stateResult.copy(showConfetti = true)
        }
        is OnboardingNoteAction.Confetti.Hide -> {
            stateResult = stateResult.copy(showConfetti = false)
        }
    }

    return stateResult
}
