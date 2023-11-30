package com.tangem.feature.swap.domain.cache

import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.ui.TokenWithBalanceExpress

interface SwapDataCache {

    fun cacheBalances(networkId: String, derivationPath: String?, balances: Map<String, SwapAmount>)

    fun getBalanceForToken(networkId: String, derivationPath: String?, symbol: String): SwapAmount?
}
