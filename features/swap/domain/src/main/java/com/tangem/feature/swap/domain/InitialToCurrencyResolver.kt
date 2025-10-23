package com.tangem.feature.swap.domain

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.swap.domain.models.ui.AccountSwapCurrency
import com.tangem.feature.swap.domain.models.ui.TokensDataStateExpress

interface InitialToCurrencyResolver {

    suspend fun tryGetFromCache(
        userWallet: UserWallet,
        initialCryptoCurrency: CryptoCurrency,
        state: TokensDataStateExpress,
        isReverseFromTo: Boolean,
    ): CryptoCurrencyStatus?

    fun tryGetWithMaxAmount(state: TokensDataStateExpress, isReverseFromTo: Boolean): CryptoCurrencyStatus?

    suspend fun tryGetFromCacheV2(
        userWallet: UserWallet,
        initialCryptoCurrency: CryptoCurrency,
        state: TokensDataStateExpress,
        isReverseFromTo: Boolean,
    ): AccountSwapCurrency?

    fun tryGetWithMaxAmountV2(state: TokensDataStateExpress, isReverseFromTo: Boolean): AccountSwapCurrency?
}