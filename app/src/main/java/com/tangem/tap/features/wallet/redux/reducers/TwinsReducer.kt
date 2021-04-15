package com.tangem.tap.features.wallet.redux.reducers

import com.tangem.tap.features.wallet.redux.TwinCardsState
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.features.wallet.redux.WalletState

class TwinsReducer {
    fun reduce(action: WalletAction.TwinsAction, state: WalletState): WalletState {
        return when (action) {
            is WalletAction.TwinsAction.SetTwinCard -> {
                state.copy(
                        twinCardsState = TwinCardsState(
                                secondCardId = action.secondCardId,
                                cardNumber = action.number,
                                showTwinOnboarding = state.twinCardsState?.showTwinOnboarding
                                        ?: false,
                                isCreatingTwinCardsAllowed = action.isCreatingTwinCardsAllowed
                        )
                )
            }
            is WalletAction.TwinsAction.ShowOnboarding -> {
                state.copy(
                        twinCardsState = state.twinCardsState?.copy(showTwinOnboarding = true)
                                ?: TwinCardsState(null, null,
                                        showTwinOnboarding = true,
                                        isCreatingTwinCardsAllowed = false)
                )
            }
            is WalletAction.TwinsAction.SetOnboardingShown -> {
                state.copy(
                        twinCardsState = state.twinCardsState?.copy(showTwinOnboarding = false)
                )
            }
        }
    }
}