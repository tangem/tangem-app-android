package com.tangem.feature.swap.domain

import com.tangem.feature.swap.domain.models.data.Currency
import com.tangem.feature.swap.domain.models.data.SwapState

interface SwapInteractor {

    suspend fun getTokensToSwap(networkId: String): List<Currency>

    suspend fun getTokenBalance(tokenId: String): String

    suspend fun givePermissionToSwap(tokenAddress: String)

    suspend fun findBestQuote(
        fromTokenAddress: String,
        toTokenAddress: String,
        amount: String,
    ): SwapState

    suspend fun onSwap(): SwapState
}
