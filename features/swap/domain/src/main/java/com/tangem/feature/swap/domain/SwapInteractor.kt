package com.tangem.feature.swap.domain

import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.cache.ExchangeCurrencies
import com.tangem.feature.swap.domain.models.domain.Currency
import com.tangem.feature.swap.domain.models.ui.FoundTokensState
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.models.ui.TokensDataState

interface SwapInteractor {

    suspend fun initTokensToSwap(initialCurrency: Currency): TokensDataState

    suspend fun onSearchToken(searchQuery: String): FoundTokensState

    fun getExchangeCurrencies(): ExchangeCurrencies?

    fun findTokenById(id: String): Currency?

    /**
     * Give permission to swap
     */
    @Throws(IllegalStateException::class)
    suspend fun givePermissionToSwap()

    @Throws(IllegalStateException::class)
    suspend fun findBestQuote(
        fromToken: Currency,
        toToken: Currency,
        amount: SwapAmount,
    ): SwapState

    @Throws(IllegalStateException::class)
    suspend fun onSwap(): SwapState

    fun getTokenDecimals(token: Currency): Int
}
