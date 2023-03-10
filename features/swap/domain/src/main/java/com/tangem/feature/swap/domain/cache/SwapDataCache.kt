package com.tangem.feature.swap.domain.cache

import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.Currency
import com.tangem.feature.swap.domain.models.ui.TokenWithBalance
import java.math.BigDecimal

interface SwapDataCache {

    fun cacheAvailableToSwapTokens(networkId: String, tokens: List<Currency>)
    fun cacheInWalletTokens(tokens: List<TokenWithBalance>)
    fun cacheLoadedTokens(tokens: List<TokenWithBalance>)
    fun cacheBalances(networkId: String, derivationPath: String?, balances: Map<String, SwapAmount>)
    fun cacheLastFeeForNetwork(fee: BigDecimal, networkId: String)
    fun getAvailableTokens(networkId: String): List<Currency>
    fun getInWalletTokens(): List<TokenWithBalance>
    fun getLoadedTokens(): List<TokenWithBalance>
    fun getBalanceForToken(networkId: String, derivationPath: String?, symbol: String): SwapAmount?
    fun getLastFeeForNetwork(networkId: String): BigDecimal?
}
