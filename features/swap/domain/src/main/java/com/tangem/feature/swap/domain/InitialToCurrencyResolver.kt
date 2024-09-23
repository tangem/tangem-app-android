package com.tangem.feature.swap.domain

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.swap.domain.models.ui.TokensDataStateExpress

interface InitialToCurrencyResolver {

    suspend fun tryGetFromCache(
        userWallet: UserWallet,
        initialCryptoCurrency: CryptoCurrency,
        state: TokensDataStateExpress,
        isReverseFromTo: Boolean,
    ): CryptoCurrencyStatus?

    fun tryGetWithMaxAmount(state: TokensDataStateExpress, isReverseFromTo: Boolean): CryptoCurrencyStatus?
}
