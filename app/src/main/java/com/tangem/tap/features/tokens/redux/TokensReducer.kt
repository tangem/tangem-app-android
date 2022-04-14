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
        is TokensAction.ResetState -> TokensState()
        is TokensAction.LoadCurrencies -> tokensState.copy(scanResponse = action.scanResponse)
        is TokensAction.LoadCurrencies.Success -> {
            tokensState.copy(currencies = action.currencies)
        }
        is TokensAction.SetAddedCurrencies -> {

            tokensState.copy(
                addedBlockchains = action.wallets.toBlockchains(action.derivationStyle),
                addedTokens = action.wallets.toTokensWithBlockchains(action.derivationStyle),
                addedWallets = action.wallets,
                derivationStyle = action.derivationStyle
            )
        }
        is TokensAction.SetNonRemovableCurrencies -> {
            tokensState.copy(
                nonRemovableBlockchains = action.wallets.toBlockchains(tokensState.derivationStyle),
                nonRemovableTokens = action.wallets.toTokensContractAddresses(),
            )
        }

        is TokensAction.AllowToAddTokens -> {
            tokensState.copy(allowToAdd = action.allow)
        }
        else -> tokensState
    }
}