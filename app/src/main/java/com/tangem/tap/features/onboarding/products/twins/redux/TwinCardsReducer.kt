package com.tangem.tap.features.onboarding.products.twins.redux

import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.twins.getTwinCardNumber
import org.rekotlin.Action

class TwinCardsReducer {
    companion object {
        fun reduce(action: Action, state: AppState): TwinCardsState = internalReduce(action, state)
    }
}

private fun internalReduce(action: Action, state: AppState): TwinCardsState {
    if (action !is TwinCardsAction) return state.twinCardsState

    var state = state.twinCardsState

    when (action) {
        is TwinCardsAction.IfTwinsPrepareState -> {
            state = if (action.scanResponse.isTangemTwins()) {
                val cardNumber = action.scanResponse.card.getTwinCardNumber() ?: return state

                TwinCardsState(
                    cardNumber = cardNumber,
                    mode = CreateTwinWalletMode.CreateWallet,
                )
            } else {
                TwinCardsState()
            }
        }
        is TwinCardsAction.SetMode -> {
            state = state.copy(
                mode = action.mode,
                userWasUnderstandIfWalletRecreate = false,
            )
        }
        is TwinCardsAction.SetStepOfScreen -> {
            state = state.copy(currentStep = action.step)
        }
        is TwinCardsAction.SetUserUnderstand -> {
            state = state.copy(userWasUnderstandIfWalletRecreate = action.isUnderstand)
        }
        is TwinCardsAction.CardsManager.Set -> {
            state = state.copy(twinCardsManager = action.manager)
        }
        TwinCardsAction.CardsManager.Release -> {
            state = state.copy(twinCardsManager = null)
        }
        is TwinCardsAction.SetPairCardId -> {
            state = state.copy(pairCardId = action.cardId)
        }
        is TwinCardsAction.SetWalletManager -> {
            state = state.copy(walletManager = action.walletManager)
        }
        is TwinCardsAction.Balance.Set -> {
            state = state.copy(walletBalance = action.balance)
        }
        is TwinCardsAction.Balance.SetCriticalError -> {
            state = state.copy(balanceCriticalError = action.error)
        }
        is TwinCardsAction.Balance.SetNonCriticalError -> {
            state = state.copy(balanceNonCriticalError = action.error)
        }
        is TwinCardsAction.Confetti.Show -> {
            state = state.copy(showConfetti = true)
        }
        is TwinCardsAction.Confetti.Hide -> {
            state = state.copy(showConfetti = false)
        }
    }

    return state
}