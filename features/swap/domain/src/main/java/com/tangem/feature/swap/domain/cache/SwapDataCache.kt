package com.tangem.feature.swap.domain.cache

import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.ui.TokenWithBalanceExpress

interface SwapDataCache {

    fun cacheInWalletTokens(tokens: List<TokenWithBalanceExpress>)
    fun cacheLoadedTokens(tokens: List<TokenWithBalanceExpress>)
    fun cacheBalances(networkId: String, derivationPath: String?, balances: Map<String, SwapAmount>)

    fun getInWalletTokens(): List<TokenWithBalanceExpress>
    fun getLoadedTokens(): List<TokenWithBalanceExpress>
    fun getBalanceForToken(networkId: String, derivationPath: String?, symbol: String): SwapAmount?
}
