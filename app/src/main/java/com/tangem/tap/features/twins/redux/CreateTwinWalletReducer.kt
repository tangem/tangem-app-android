package com.tangem.tap.features.twins.redux

import com.tangem.tap.features.details.redux.DetailsState

class CreateTwinWalletReducer {
    companion object {
        fun handle(
            action: CreateTwinWalletAction, state: DetailsState
        ): DetailsState {
            return when (action) {
                is CreateTwinWalletAction.ShowWarning -> {
                    state.copy(createTwinWalletState = CreateTwinWalletState(
                        scanResponse = null,
                        twinCardNumber = action.twinCardNumber
                                ?: state.createTwinWalletState?.twinCardNumber,
                        createTwinWallet = action.createTwinWallet,
                        showAlert = false,
                        allowRecreatingWallet = state.createTwinWalletState?.allowRecreatingWallet
                    ))
                }
                CreateTwinWalletAction.NotEmpty -> state
                CreateTwinWalletAction.ShowAlert -> {
                    state.copy(createTwinWalletState = state.createTwinWalletState?.copy(
                        showAlert = true
                    ))
                }
                CreateTwinWalletAction.HideAlert -> {
                    state.copy(createTwinWalletState = state.createTwinWalletState?.copy(
                        showAlert = false
                    ))
                }
                is CreateTwinWalletAction.Proceed -> {
                    state
                }
                CreateTwinWalletAction.Cancel -> state
                CreateTwinWalletAction.Cancel.Confirm -> state
                is CreateTwinWalletAction.LaunchFirstStep -> state

                CreateTwinWalletAction.LaunchFirstStep.Success -> {
                    state.copy(createTwinWalletState = state.createTwinWalletState?.copy(
                        step = CreateTwinWalletStep.SecondStep
                    ))
                }
                CreateTwinWalletAction.LaunchFirstStep.Failure -> state

                is CreateTwinWalletAction.LaunchSecondStep -> state
                CreateTwinWalletAction.LaunchSecondStep.Success ->
                    state.copy(createTwinWalletState = state.createTwinWalletState?.copy(
                        step = CreateTwinWalletStep.ThirdStep
                    ))
                CreateTwinWalletAction.LaunchSecondStep.Failure -> {
                    state
                }
                is CreateTwinWalletAction.LaunchThirdStep -> {
                    state
                }
                is CreateTwinWalletAction.LaunchThirdStep.Success -> {
                    state
                }
                CreateTwinWalletAction.LaunchThirdStep.Failure -> {
                    state
                }
            }
        }

    }
}