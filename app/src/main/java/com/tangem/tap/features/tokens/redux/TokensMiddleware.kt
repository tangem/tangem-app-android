package com.tangem.tap.features.tokens.redux

import com.tangem.tap.common.redux.AppState
import com.tangem.tap.currenciesRepository
import com.tangem.tap.domain.TapWorkarounds.isTestCard
import com.tangem.tap.store
import org.rekotlin.Middleware

class TokensMiddleware {

    val tokensMiddleware: Middleware<AppState> = { dispatch, state ->
        { next ->
            { action ->
                when (action) {
                    is TokensAction.LoadCurrencies -> {
                        val card = state()?.globalState?.scanResponse?.card
                        val isTestcard = card?.isTestCard ?: false
                        val tokens = currenciesRepository.getPopularTokens(isTestcard)
                        val blockchains = currenciesRepository.getBlockchains(
                            cardFirmware = card?.firmwareVersion,
                            isTestNet = isTestcard
                        )
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