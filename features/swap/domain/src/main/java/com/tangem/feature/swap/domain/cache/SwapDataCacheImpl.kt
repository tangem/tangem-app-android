package com.tangem.feature.swap.domain.cache

import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.cache.ExchangeCurrencies
import com.tangem.feature.swap.domain.models.cache.SwapDataHolder
import com.tangem.feature.swap.domain.models.domain.ApproveModel
import com.tangem.feature.swap.domain.models.domain.Currency
import com.tangem.feature.swap.domain.models.domain.QuoteModel
import com.tangem.feature.swap.domain.models.domain.SwapDataModel

class SwapDataCacheImpl : SwapDataCache {

    private var lastDataForSwap: SwapDataHolder = SwapDataHolder()
    private val availableTokensForNetwork: MutableMap<String, List<Currency>> = mutableMapOf()
    private val lastInWalletTokens = mutableListOf<Currency>()
    private val lastLoadedTokens = mutableListOf<Currency>()

    override fun cacheQuoteData(
        quoteModel: QuoteModel,
    ) {
        lastDataForSwap = lastDataForSwap.copy(quoteModel = quoteModel)
    }

    override fun cacheExchangeCurrencies(fromToken: Currency, toToken: Currency) {
        lastDataForSwap =
            lastDataForSwap.copy(
                exchangeCurrencies = ExchangeCurrencies(
                    fromCurrency = fromToken,
                    toCurrency = toToken,
                ),
            )
    }

    override fun cacheInWalletTokens(tokens: List<Currency>) {
        lastInWalletTokens.clear()
        lastInWalletTokens.addAll(tokens)
    }

    override fun cacheLoadedTokens(tokens: List<Currency>) {
        lastLoadedTokens.clear()
        lastLoadedTokens.addAll(tokens)
    }

    override fun getInWalletTokens(): List<Currency> {
        return lastInWalletTokens
    }

    override fun getLoadedTokens(): List<Currency> {
        return lastLoadedTokens
    }

    override fun cacheSwapData(swapDataModel: SwapDataModel) {
        lastDataForSwap = lastDataForSwap.copy(swapModel = swapDataModel)
    }

    override fun getLastSwapData(): SwapDataModel? {
        return lastDataForSwap.swapModel
    }

    override fun cacheApproveTransactionData(approve: ApproveModel) {
        lastDataForSwap = lastDataForSwap.copy(approveTxModel = approve)
    }

    override fun getExchangeCurrencies(): ExchangeCurrencies? {
        return lastDataForSwap.exchangeCurrencies
    }

    override fun cacheAvailableToSwapTokens(networkId: String, tokens: List<Currency>) {
        availableTokensForNetwork[networkId] = tokens
    }

    override fun cacheNetworkId(networkId: String) {
        lastDataForSwap = lastDataForSwap.copy(networkId = networkId)
    }

    override fun cacheAmountToSwap(amount: SwapAmount) {
        lastDataForSwap = lastDataForSwap.copy(amountToSwap = amount)
    }

    override fun getNetworkId(): String? {
        return lastDataForSwap.networkId
    }

    override fun getAvailableTokens(networkId: String): List<Currency> {
        return availableTokensForNetwork.getOrElse(networkId) { emptyList() }
    }

    override fun getApproveTransactionData(): ApproveModel? {
        return lastDataForSwap.approveTxModel
    }

    override fun getLastQuote(): QuoteModel? {
        return lastDataForSwap.quoteModel
    }

    override fun getAmountToSwap(): SwapAmount? {
        return lastDataForSwap.amountToSwap
    }
}
