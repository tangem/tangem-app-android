package com.tangem.feature.swap.domain.cache

import com.tangem.feature.swap.domain.models.SwapDataHolder
import com.tangem.feature.swap.domain.models.data.Currency
import com.tangem.feature.swap.domain.models.data.SwapState.QuoteModel

class SwapDataCacheImpl : SwapDataCache {

    private var lastDataForSwap: SwapDataHolder = SwapDataHolder()
    private val availableTokensForNetwork: MutableMap<String, List<Currency>> = mutableMapOf()

    override fun cacheSwapParams(quoteModel: QuoteModel, amount: String) {
        lastDataForSwap = lastDataForSwap.copy(quoteModel = quoteModel, amountToSwap = amount)
    }

    override fun cacheAvailableToSwapTokens(networkId: String, tokens: List<Currency>) {
        availableTokensForNetwork[networkId] = tokens
    }

    override fun cacheNetworkId(networkId: String) {
        lastDataForSwap = lastDataForSwap.copy(networkId = networkId)
    }

    override fun getNetworkId(): String? {
        return lastDataForSwap.networkId
    }

    override fun getAvailableTokens(networkId: String): List<Currency> {
        return availableTokensForNetwork.getOrElse(networkId) { emptyList() }
    }

    override fun getLastQuote(): QuoteModel? {
        return lastDataForSwap.quoteModel
    }

    override fun getAmountToSwap(): String? {
        return lastDataForSwap.amountToSwap
    }
}
