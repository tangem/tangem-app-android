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
        is TokensAction.LoadCurrencies -> {
            val loadingState = if (tokensState.currencies.isEmpty()) {
                LoadCoinsState.LOADING
            } else {
                LoadCoinsState.LOADED
            }
            tokensState.copy(
                scanResponse = action.scanResponse,
                loadCoinsState = loadingState
            )
        }
        is TokensAction.LoadCurrencies.Success -> {
            tokensState.copy(
                currencies = tokensState.currencies + action.currencies,
                needToLoadMore = action.loadMore,
                pageToLoad = tokensState.pageToLoad + 1,
                loadCoinsState = LoadCoinsState.LOADED
            )
        }
        is TokensAction.SetAddedCurrencies -> {

            tokensState.copy(
                addedBlockchains = action.wallets.toNonCustomBlockchains(action.derivationStyle),
                addedTokens = action.wallets.toNonCustomTokensWithBlockchains(action.derivationStyle),
                addedWallets = action.wallets,
                derivationStyle = action.derivationStyle
            )
        }
        is TokensAction.SetNonRemovableCurrencies -> {
            tokensState.copy(
                nonRemovableBlockchains = action.wallets.toNonCustomBlockchains(tokensState.derivationStyle),
                nonRemovableTokens = action.wallets.toTokensContractAddresses(),
            )
        }

        is TokensAction.AllowToAddTokens -> {
            tokensState.copy(allowToAdd = action.allow)
        }
        is TokensAction.SetSearchInput -> {
            tokensState.copy(
                searchInput = action.searchInput.ifBlank { null },
                needToLoadMore = true,
                currencies = emptyList(),
                pageToLoad = 0,
                loadCoinsState = LoadCoinsState.LOADING
            )
        }
        else -> tokensState
    }
}