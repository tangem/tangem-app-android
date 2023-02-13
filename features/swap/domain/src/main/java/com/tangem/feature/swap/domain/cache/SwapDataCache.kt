package com.tangem.feature.swap.domain.cache

import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.Currency
import java.math.BigDecimal

interface SwapDataCache {

    fun cacheAvailableToSwapTokens(networkId: String, tokens: List<Currency>)
    fun cacheInWalletTokens(tokens: List<Currency>)
    fun cacheLoadedTokens(tokens: List<Currency>)
    fun cacheBalances(balances: Map<String, SwapAmount>)
    fun cacheLastFeeForNetwork(fee: BigDecimal, networkId: String)
    fun getAvailableTokens(networkId: String): List<Currency>
    fun getInWalletTokens(): List<Currency>
    fun getLoadedTokens(): List<Currency>
    fun getBalanceForToken(symbol: String): SwapAmount?
    fun getLastFeeForNetwork(networkId: String): BigDecimal?
}