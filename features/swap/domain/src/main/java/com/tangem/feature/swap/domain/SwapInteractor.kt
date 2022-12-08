package com.tangem.feature.swap.domain

import com.tangem.feature.swap.domain.models.data.Currency
import com.tangem.feature.swap.domain.models.data.QuoteModel
import com.tangem.feature.swap.domain.models.SwapResultModel

interface SwapInteractor {

    suspend fun getTokensToSwap(networkId: String): List<Currency>

    suspend fun getTokenBalance(): String

    suspend fun findBestQuote(
        fromTokenAddress: String,
        toTokenAddress: String,
        amount: String,
    ): QuoteModel

    suspend fun onSwap(): SwapResultModel
}