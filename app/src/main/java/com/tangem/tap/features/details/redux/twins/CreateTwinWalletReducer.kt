package com.tangem.tap.features.details.redux.twins

import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.details.redux.DetailsState

class CreateTwinWalletReducer {
    companion object {
        fun handle(
                action: DetailsAction.CreateTwinWalletAction, state: DetailsState
        ): DetailsState {
            return when (action) {
                DetailsAction.CreateTwinWalletAction.ShowWarning -> {
                    state
                }
                DetailsAction.CreateTwinWalletAction.ShowAlert -> {
                    state.copy(createTwinWalletState = state.createTwinWalletState?.copy(
                            showAlert = true
                    ))
                }
                DetailsAction.CreateTwinWalletAction.HideAlert -> {
                    state.copy(createTwinWalletState = state.createTwinWalletState?.copy(
                            showAlert = false
                    ))
                }
                is DetailsAction.CreateTwinWalletAction.Proceed -> {
                    state.copy(createTwinWalletState = CreateTwinWalletState(
                            scanResponse = null,
                            twinCardNumber = action.twinCardNumber
                                    ?: state.createTwinWalletState?.twinCardNumber,
                            createTwinWallet = action.createTwinWallet,
                            showAlert = false,
                            step = CreateTwinWalletStep.FirstStep,
                    ))
                }
                DetailsAction.CreateTwinWalletAction.Cancel -> state
                DetailsAction.CreateTwinWalletAction.Cancel.Confirm -> state
                is DetailsAction.CreateTwinWalletAction.LaunchFirstStep -> state

                DetailsAction.CreateTwinWalletAction.LaunchFirstStep.Success -> {
                    state.copy(createTwinWalletState = state.createTwinWalletState?.copy(
                            step = CreateTwinWalletStep.SecondStep
                    ))
                }
                DetailsAction.CreateTwinWalletAction.LaunchFirstStep.Failure -> state

                is DetailsAction.CreateTwinWalletAction.LaunchSecondStep -> state
                DetailsAction.CreateTwinWalletAction.LaunchSecondStep.Success ->
                    state.copy(createTwinWalletState = state.createTwinWalletState?.copy(
                            step = CreateTwinWalletStep.ThirdStep
                    ))
                DetailsAction.CreateTwinWalletAction.LaunchSecondStep.Failure -> {
                    state
                }
                is DetailsAction.CreateTwinWalletAction.LaunchThirdStep -> {
                    state
                }
                is DetailsAction.CreateTwinWalletAction.LaunchThirdStep.Success -> {
                    state
                }
                DetailsAction.CreateTwinWalletAction.LaunchThirdStep.Failure -> {
                    state
                }
            }
        }

    }
}