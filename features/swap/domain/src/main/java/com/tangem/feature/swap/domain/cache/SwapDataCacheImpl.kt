package com.tangem.feature.swap.domain.cache

import com.tangem.feature.swap.domain.models.domain.Currency
import java.math.BigDecimal

class SwapDataCacheImpl : SwapDataCache {

    private val availableTokensForNetwork: MutableMap<String, List<Currency>> = mutableMapOf()
    private val feesForNetworks: MutableMap<String, BigDecimal> = mutableMapOf()
    private val lastInWalletTokens = mutableListOf<Currency>()
    private val lastLoadedTokens = mutableListOf<Currency>()

    override fun cacheLastFeeForNetwork(fee: BigDecimal, networkId: String) {
        feesForNetworks[networkId] = fee
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

    override fun cacheAvailableToSwapTokens(networkId: String, tokens: List<Currency>) {
        availableTokensForNetwork[networkId] = tokens
    }

    override fun getLastFeeForNetwork(networkId: String): BigDecimal? {
        return feesForNetworks[networkId]
    }

    override fun getAvailableTokens(networkId: String): List<Currency> {
        return availableTokensForNetwork.getOrElse(networkId) { emptyList() }
    }
}
