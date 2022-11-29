package com.tangem.feature.swap.domain

import com.tangem.feature.swap.domain.models.Currency
import com.tangem.feature.swap.domain.models.QuoteModel

interface SwapRepository {

    suspend fun getExchangeableTokens(networkId: String): List<Currency>

    suspend fun findBestQuote(
        fromTokenAddress: String,
        toTokenAddress: String,
        amount: String,
    ): QuoteModel

    suspend fun addressForTrust(): String
}