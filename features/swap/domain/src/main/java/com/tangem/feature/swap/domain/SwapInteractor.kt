package com.tangem.feature.swap.domain

import com.tangem.feature.swap.domain.models.data.Currency
import com.tangem.feature.swap.domain.models.data.SwapAmount
import com.tangem.feature.swap.domain.models.data.SwapState

interface SwapInteractor {

    suspend fun getTokensToSwap(networkId: String): List<Currency>

    suspend fun getTokenBalance(tokenId: String): String

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