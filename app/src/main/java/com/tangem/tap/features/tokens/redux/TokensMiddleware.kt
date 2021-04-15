package com.tangem.tap.features.tokens.redux

import com.tangem.tap.common.redux.AppState
import com.tangem.tap.currenciesRepository
import com.tangem.tap.features.tokens.ui.adapters.CurrencyListItem
import com.tangem.tap.store
import org.rekotlin.Middleware

class TokensMiddleware {

    val tokensMiddleware: Middleware<AppState> = { dispatch, state ->
        { next ->
            { action ->
                when (action) {
                    is TokensAction.LoadCurrencies -> {
                        val tokens = currenciesRepository.getPopularTokens()
                        val blockchains = currenciesRepository.getBlockchains()
                        val currencies = CurrencyListItem.createListOfCurrencies(
                                blockchains, tokens
                        )
                        store.dispatch(TokensAction.LoadCurrencies.Success(currencies))
                    }
                }
                next(action)
            }
        }
    }
}