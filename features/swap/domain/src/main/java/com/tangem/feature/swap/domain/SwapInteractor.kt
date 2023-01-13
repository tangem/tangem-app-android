package com.tangem.feature.swap.domain

import com.tangem.feature.swap.domain.models.Currency
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.SwapState

interface SwapInteractor {

    suspend fun getTokensToSwap(networkId: String): List<Currency>

    /**
     * Give permission to swap
     *
     * @param tokenToApprove use token which you want to swap
     */
    @Throws(IllegalStateException::class)
    suspend fun givePermissionToSwap(tokenToApprove: Currency)

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