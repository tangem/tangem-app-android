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

    val twinCardsState = state.twinCardsState
    return when (action) {
        is TwinCardsAction.SetTwinCard -> {
            twinCardsState.copy(
                cardNumber = action.number,
                secondCardId = action.secondCardId,
                showTwinOnboarding = state.twinCardsState.showTwinOnboarding,
                isCreatingTwinCardsAllowed = action.isCreatingTwinCardsAllowed,
            )
        }
        is TwinCardsAction.ShowOnboarding -> {
            state.twinCardsState.copy(showTwinOnboarding = true)
        }
        is TwinCardsAction.SetOnboardingShown -> {
            twinCardsState.copy(showTwinOnboarding = false)
        }
        is TwinCardsAction.CreateWallet.Create -> {
            val prevState = twinCardsState.createWalletState
            twinCardsState.copy(createWalletState = CreateTwinWalletState(
                prevState?.scanResponse,
                prevState?.number ?: action.number,
                prevState?.mode ?: action.createTwinWalletMode,
            ))
        }
        TwinCardsAction.CreateWallet.LaunchFirstStep.Success -> {
            twinCardsState.copy(createWalletState = twinCardsState.createWalletState?.copy(
                step = CreateTwinWalletStep.SecondStep
            ))
        }
        TwinCardsAction.CreateWallet.LaunchSecondStep.Success ->
            twinCardsState.copy(createWalletState = twinCardsState.createWalletState?.copy(
                step = CreateTwinWalletStep.ThirdStep
            ))
        TwinCardsAction.CreateWallet.ShowAlert -> twinCardsState
        TwinCardsAction.CreateWallet.HideAlert -> twinCardsState
        is TwinCardsAction.CreateWallet.Proceed -> twinCardsState
        TwinCardsAction.CreateWallet.NotEmpty -> twinCardsState
        TwinCardsAction.CreateWallet.Cancel -> twinCardsState
        TwinCardsAction.CreateWallet.Cancel.Confirm -> twinCardsState
        is TwinCardsAction.CreateWallet.LaunchFirstStep -> twinCardsState
        TwinCardsAction.CreateWallet.LaunchFirstStep.Failure -> twinCardsState
        is TwinCardsAction.CreateWallet.LaunchSecondStep -> twinCardsState
        TwinCardsAction.CreateWallet.LaunchSecondStep.Failure -> twinCardsState
        is TwinCardsAction.CreateWallet.LaunchThirdStep -> twinCardsState
        is TwinCardsAction.CreateWallet.LaunchThirdStep.Success -> twinCardsState
        TwinCardsAction.CreateWallet.LaunchThirdStep.Failure -> twinCardsState
    }

}