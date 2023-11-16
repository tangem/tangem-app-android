package com.tangem.feature.swap.domain.cache

import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.Currency
import com.tangem.feature.swap.domain.models.ui.TokenWithBalanceExpress
import java.math.BigDecimal

interface SwapDataCache {

    fun cacheAvailableToSwapTokens(networkId: String, tokens: List<Currency>)
    fun cacheInWalletTokens(tokens: List<TokenWithBalanceExpress>)
    fun cacheLoadedTokens(tokens: List<TokenWithBalanceExpress>)
    fun cacheBalances(networkId: String, derivationPath: String?, balances: Map<String, SwapAmount>)
    fun cacheLastFeeForNetwork(fee: BigDecimal, networkId: String)
    fun getAvailableTokens(networkId: String): List<Currency>
    fun getInWalletTokens(): List<TokenWithBalanceExpress>
    fun getLoadedTokens(): List<TokenWithBalanceExpress>
    fun getBalanceForToken(networkId: String, derivationPath: String?, symbol: String): SwapAmount?
    fun getLastFeeForNetwork(networkId: String): BigDecimal?
}