package com.tangem.feature.swap.domain.cache

import com.tangem.feature.swap.domain.models.ApproveModel
import com.tangem.feature.swap.domain.models.Currency
import com.tangem.feature.swap.domain.models.QuoteModel
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.SwapDataModel
import com.tangem.feature.swap.domain.models.cache.ExchangeCurrencies

interface SwapDataCache {

    fun cacheNetworkId(networkId: String)
    fun cacheSwapParams(quoteModel: QuoteModel, amount: SwapAmount, fromCurrency: Currency, toCurrency: Currency)
    fun cacheApproveTransactionData(approve: ApproveModel)
    fun cacheSwapData(swapDataModel: SwapDataModel)
    fun cacheAvailableToSwapTokens(networkId: String, tokens: List<Currency>)
    fun getAvailableTokens(networkId: String): List<Currency>
    fun getApproveTransactionData(): ApproveModel?
    fun getLastQuote(): QuoteModel?
    fun getLastSwapData(): SwapDataModel?
    fun getAmountToSwap(): SwapAmount?
    fun getNetworkId(): String?
    fun getExchangeCurrencies(): ExchangeCurrencies?
}
