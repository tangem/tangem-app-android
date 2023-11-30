package com.tangem.feature.swap.domain.cache

import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.ui.TokenWithBalanceExpress

class SwapDataCacheImpl : SwapDataCache {

    private val tokensBalances: MutableMap<String, Map<String, SwapAmount>> = mutableMapOf()

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
