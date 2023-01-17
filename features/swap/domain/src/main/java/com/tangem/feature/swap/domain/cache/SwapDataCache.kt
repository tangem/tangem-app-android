package com.tangem.feature.swap.domain.cache

import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.cache.ExchangeCurrencies
import com.tangem.feature.swap.domain.models.domain.ApproveModel
import com.tangem.feature.swap.domain.models.domain.Currency
import com.tangem.feature.swap.domain.models.domain.QuoteModel
import com.tangem.feature.swap.domain.models.domain.SwapDataModel

interface SwapDataCache {

    fun cacheNetworkId(networkId: String)
    fun cacheQuoteData(quoteModel: QuoteModel)
    fun cacheSwapData(swapDataModel: SwapDataModel)
    fun cacheAmountToSwap(amount: SwapAmount)
    fun cacheApproveTransactionData(approve: ApproveModel)
    fun cacheAvailableToSwapTokens(networkId: String, tokens: List<Currency>)
    fun cacheInWalletTokens(tokens: List<Currency>)
    fun cacheLoadedTokens(tokens: List<Currency>)
    fun cacheExchangeCurrencies(fromToken: Currency, toToken: Currency)
    fun getAvailableTokens(networkId: String): List<Currency>
    fun getApproveTransactionData(): ApproveModel?
    fun getLastQuote(): QuoteModel?
    fun getLastSwapData(): SwapDataModel?
    fun getNetworkId(): String?
    fun getAmountToSwap(): SwapAmount?
    fun getExchangeCurrencies(): ExchangeCurrencies?
    fun getInWalletTokens(): List<Currency>
    fun getLoadedTokens(): List<Currency>
}
