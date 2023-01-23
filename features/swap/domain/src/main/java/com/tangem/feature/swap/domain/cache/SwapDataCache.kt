package com.tangem.feature.swap.domain.cache

import com.tangem.feature.swap.domain.models.domain.Currency
import java.math.BigDecimal

interface SwapDataCache {

    fun cacheAvailableToSwapTokens(networkId: String, tokens: List<Currency>)
    fun cacheInWalletTokens(tokens: List<Currency>)
    fun cacheLoadedTokens(tokens: List<Currency>)
    fun cacheLastFeeForNetwork(fee: BigDecimal, networkId: String)
    fun getAvailableTokens(networkId: String): List<Currency>
    fun getInWalletTokens(): List<Currency>
    fun getLoadedTokens(): List<Currency>
    fun getLastFeeForNetwork(networkId: String): BigDecimal?
}
