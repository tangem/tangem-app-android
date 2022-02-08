package com.tangem.tap.features.tokens.redux

import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.tokens.CardCurrencies
import com.tangem.tap.features.wallet.redux.Currency
import com.tangem.tap.features.wallet.redux.WalletData
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
        is TokensAction.LoadCurrencies.Success -> {
            tokensState.copy(currencies = action.currencies, shownCurrencies = action.currencies)
        }
        is TokensAction.SetAddedCurrencies -> {
            tokensState.copy(addedCurrencies = action.wallets.toCardCurrencies())
        }
        is TokensAction.ToggleShowTokensForBlockchain -> {
            if (action.isShown) {
                val shownCurrencies = tokensState.shownCurrencies
                    .removeTokensForBlockchain(action.blockchain)
                shownCurrencies.toggleHeaderContentShownValue(action.blockchain)
                tokensState.copy(shownCurrencies = shownCurrencies)
            } else {
                val shownCurrencies = tokensState.shownCurrencies
                    .addTokensForBlockchain(action.blockchain, tokensState.currencies)
                shownCurrencies.toggleHeaderContentShownValue(action.blockchain)
                tokensState.copy(shownCurrencies = shownCurrencies)
            }
        }
        is TokensAction.OpenAllTokens -> {
            val shownCurrencies = tokensState.currencies
            shownCurrencies.forEach {  if (it is CurrencyListItem.TitleListItem) it.isContentShown = true}
            tokensState.copy(shownCurrencies = tokensState.currencies)
        }
        else -> tokensState
    }
}

private fun List<WalletData>.toCardCurrencies(): CardCurrencies {
    val tokens = mapNotNull { (it.currency as? Currency.Token)?.token }.distinct()
    val blockchains = mapNotNull { (it.currency as? Currency.Blockchain)?.blockchain }.distinct()
    return CardCurrencies(tokens = tokens, blockchains = blockchains)
}
