package com.tangem.feature.swap.domain

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.swap.domain.models.ui.TokensDataStateExpress

interface InitialToCurrencyResolver {

    suspend fun tryGetFromCache(
        initialCryptoCurrency: CryptoCurrency,
        state: TokensDataStateExpress,
    ): CryptoCurrencyStatus?

    fun tryGetWithMaxAmount(state: TokensDataStateExpress): CryptoCurrencyStatus?
}
