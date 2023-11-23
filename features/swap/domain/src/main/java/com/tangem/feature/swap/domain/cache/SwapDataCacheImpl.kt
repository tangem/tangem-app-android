package com.tangem.feature.swap.domain.cache

import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.ui.TokenWithBalanceExpress

class SwapDataCacheImpl : SwapDataCache {

    private val tokensBalances: MutableMap<String, Map<String, SwapAmount>> = mutableMapOf()
    private val lastInWalletTokens = mutableListOf<TokenWithBalanceExpress>()
    private val lastLoadedTokens = mutableListOf<TokenWithBalanceExpress>()

    override fun cacheInWalletTokens(tokens: List<TokenWithBalanceExpress>) {
        lastInWalletTokens.clear()
        lastInWalletTokens.addAll(tokens)
    }

    override fun cacheLoadedTokens(tokens: List<TokenWithBalanceExpress>) {
        lastLoadedTokens.clear()
        lastLoadedTokens.addAll(tokens)
    }

    override fun getInWalletTokens(): List<TokenWithBalanceExpress> {
        return lastInWalletTokens
    }

    override fun getLoadedTokens(): List<TokenWithBalanceExpress> {
        return lastLoadedTokens
    }

    override fun getBalanceForToken(networkId: String, derivationPath: String?, symbol: String): SwapAmount? {
        return tokensBalances[createKeyFrom(networkId, derivationPath)]?.get(symbol)
    }

    override fun cacheBalances(networkId: String, derivationPath: String?, balances: Map<String, SwapAmount>) {
        tokensBalances[createKeyFrom(networkId, derivationPath)] = balances
    }

    private fun createKeyFrom(networkId: String, derivationPath: String?): String {
        return "$networkId;$derivationPath"
    }
}