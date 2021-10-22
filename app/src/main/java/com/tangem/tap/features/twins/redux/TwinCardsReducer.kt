package com.tangem.tap.features.twins.redux

import com.tangem.tap.common.redux.AppState
import org.rekotlin.Action

class TwinCardsReducer {
    companion object {
        fun reduce(action: Action, state: AppState): TwinCardsState = internalReduce(action, state)
    }
}

private fun internalReduce(action: Action, state: AppState): TwinCardsState {
    if (action !is TwinCardsAction) return state.twinCardsState

    val state = state.twinCardsState
    return when (action) {
        is TwinCardsAction.SetResources -> {
            state.copy(resources = action.resources)
        }
        is TwinCardsAction.SetTwinCard -> {
            state.copy(
                cardNumber = action.number,
                secondCardId = action.secondCardId,
                showTwinOnboarding = state.showTwinOnboarding,
                isCreatingTwinCardsAllowed = action.isCreatingTwinCardsAllowed,
            )
        }
        is TwinCardsAction.CardsManager.Set -> {
            state.copy(twinCardsManager = action.manager)
        }
        TwinCardsAction.CardsManager.Release -> {
            state.copy(twinCardsManager = null)
        }
        is TwinCardsAction.ShowOnboarding -> {
            state.copy(showTwinOnboarding = true)
        }
        is TwinCardsAction.SetOnboardingShown -> {
            state.copy(showTwinOnboarding = false)
        }
        is TwinCardsAction.Wallet.Create -> {
            val prevState = state.createWalletState
            state.copy(createWalletState = CreateTwinWalletState(
                prevState?.scanResponse,
                prevState?.number ?: action.number,
                prevState?.mode ?: action.createTwinWalletMode,
            ))
        }
        TwinCardsAction.Wallet.LaunchFirstStep.Success -> {
            state.copy(createWalletState = state.createWalletState?.copy(
                step = CreateTwinWalletStep.SecondStep
            ))
        }
        TwinCardsAction.Wallet.LaunchSecondStep.Success ->
            state.copy(createWalletState = state.createWalletState?.copy(
                step = CreateTwinWalletStep.ThirdStep
            ))
        else -> state
    }

}