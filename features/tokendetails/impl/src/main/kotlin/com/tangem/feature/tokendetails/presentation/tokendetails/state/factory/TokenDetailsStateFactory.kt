package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import arrow.core.Either
import com.tangem.common.Provider
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.error.CurrencyError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents

internal class TokenDetailsStateFactory(
    private val currentStateProvider: Provider<TokenDetailsState>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val clickIntents: TokenDetailsClickIntents,
) {

    private val skeletonStateConverter by lazy {
        TokenDetailsSkeletonStateConverter(clickIntents = clickIntents)
    }

    private val tokenDetailsLoadedBalanceConverter by lazy {
        TokenDetailsLoadedBalanceConverter(
            currentStateProvider = currentStateProvider,
            appCurrencyProvider = appCurrencyProvider,
        )
    }

    fun getInitialState(cryptoCurrency: CryptoCurrency): TokenDetailsState {
        return skeletonStateConverter.convert(
            TokenDetailsSkeletonStateConverter.SkeletonModel(cryptoCurrency = cryptoCurrency),
        )
    }

    fun getCurrencyLoadedBalanceState(
        cryptoCurrencyEither: Either<CurrencyError, CryptoCurrencyStatus>,
    ): TokenDetailsState {
        return tokenDetailsLoadedBalanceConverter.convert(cryptoCurrencyEither)
    }
}