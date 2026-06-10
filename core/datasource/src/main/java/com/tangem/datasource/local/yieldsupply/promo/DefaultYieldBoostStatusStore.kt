package com.tangem.datasource.local.yieldsupply.promo

import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.models.YieldBoostStatus

internal class DefaultYieldBoostStatusStore(
    private val dataStore: RuntimeSharedStore<Map<UserWalletId, YieldBoostStatus>>,
) : YieldBoostStatusStore {

    override suspend fun getSyncOrNull(userWalletId: UserWalletId): YieldBoostStatus? {
        return dataStore.getSyncOrNull()?.get(userWalletId)
    }

    override suspend fun store(userWalletId: UserWalletId, value: YieldBoostStatus) {
        dataStore.update(emptyMap()) { current ->
            current + (userWalletId to value)
        }
    }
}