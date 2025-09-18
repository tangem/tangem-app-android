package com.tangem.data.tokens.repository

import com.tangem.datasource.local.token.YieldSupplyWarningActionStore
import com.tangem.domain.tokens.repository.YieldSupplyWarningsViewedRepository

internal class DefaultYieldSupplyWarningsViewedRepository(
    private val yieldSupplyWarningActionStore: YieldSupplyWarningActionStore,
) : YieldSupplyWarningsViewedRepository {

    override suspend fun getViewedWarnings(): Set<String> {
        return yieldSupplyWarningActionStore.getSync()
    }

    override suspend fun view(symbol: String) {
        yieldSupplyWarningActionStore.store(symbol)
    }
}