package com.tangem.feature.swap.domain

import com.tangem.feature.swap.domain.models.SwapResultModel
import com.tangem.feature.swap.domain.models.data.Currency
import com.tangem.feature.swap.domain.models.data.QuoteModel

interface SwapInteractor {

    suspend fun getTokensToSwap(networkId: String): List<Currency>

    suspend fun getTokenBalance(tokenId: String): String

    suspend fun findBestQuote(
        fromTokenAddress: String,
        toTokenAddress: String,
        amount: String,
    ): QuoteModel

    suspend fun onSwap(): SwapResultModel
}