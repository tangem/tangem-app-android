package com.tangem.feature.swap.domain.cache

import com.tangem.feature.swap.domain.models.data.Currency
import com.tangem.feature.swap.domain.models.data.SwapState.QuoteModel

interface SwapDataCache {

    fun cacheNetworkId(networkId: String)
    fun cacheSwapParams(quoteModel: QuoteModel, amount: String)
    fun cacheAvailableToSwapTokens(networkId: String, tokens: List<Currency>)
    fun getAvailableTokens(networkId: String): List<Currency>
    fun getLastQuote(): QuoteModel?
    fun getAmountToSwap(): String?
    fun getNetworkId(): String?
}
