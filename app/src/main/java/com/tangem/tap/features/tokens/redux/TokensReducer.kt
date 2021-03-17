package com.tangem.tap.features.tokens.redux

import com.tangem.tap.common.redux.AppState
import org.rekotlin.Action

class TokensReducer {
    companion object {
        fun reduce(action: Action, state: AppState): TokensState = internalReduce(action, state)
    }
}

private fun internalReduce(action: Action, state: AppState): TokensState {
    if (action !is TokensAction) return state.tokensState

    val tokensState = state.tokensState
    return when (action) {
        is TokensAction.LoadCurrencies.Success -> {
            tokensState.copy(currencies = action.currencies)
        }
        is TokensAction.SetAddedCurrencies -> {
            tokensState.copy(addedCurrencies = action.wallets.mapNotNull { it.currencyData.currencySymbol })
        }
        is TokensAction.LoadCardTokens.Success -> {
            tokensState.copy(addedTokens = LinkedHashSet(
                    action.tokens.map { TokenWithAmount(it, null) }
            ))
        }
        else -> tokensState
    }
}