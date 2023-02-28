package com.tangem.feature.swap.domain.cache

import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.Currency
import com.tangem.feature.swap.domain.models.ui.TokenWithBalance
import java.math.BigDecimal

class SwapDataCacheImpl : SwapDataCache {

    private val availableTokensForNetwork: MutableMap<String, List<Currency>> = mutableMapOf()
    private val feesForNetworks: MutableMap<String, BigDecimal> = mutableMapOf()
    private val tokensBalances: MutableMap<String, SwapAmount> = mutableMapOf()
    private val lastInWalletTokens = mutableListOf<TokenWithBalance>()
    private val lastLoadedTokens = mutableListOf<TokenWithBalance>()

    override fun cacheLastFeeForNetwork(fee: BigDecimal, networkId: String) {
        feesForNetworks[networkId] = fee
    }

    override fun cacheInWalletTokens(tokens: List<TokenWithBalance>) {
        lastInWalletTokens.clear()
        lastInWalletTokens.addAll(tokens)
    }

    override fun cacheLoadedTokens(tokens: List<TokenWithBalance>) {
        lastLoadedTokens.clear()
        lastLoadedTokens.addAll(tokens)
    }

    override fun getInWalletTokens(): List<TokenWithBalance> {
        return lastInWalletTokens
    }

    override fun getLoadedTokens(): List<TokenWithBalance> {
        return lastLoadedTokens
    }

    override fun getBalanceForToken(symbol: String): SwapAmount? {
        return tokensBalances[symbol]
    }

    override fun cacheBalances(balances: Map<String, SwapAmount>) {
        tokensBalances.putAll(balances)
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