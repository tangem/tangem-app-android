package com.tangem.datasource.local.yieldsupply.promo

import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.models.YieldBoostPromo

internal class DefaultYieldBoostPromoStore(
    private val dataStore: RuntimeSharedStore<Map<UserWalletId, YieldBoostPromo>>,
) : YieldBoostPromoStore {

    override suspend fun getSyncOrNull(userWalletId: UserWalletId): YieldBoostPromo? {
        return dataStore.getSyncOrNull()?.get(userWalletId)
    }

    override suspend fun store(userWalletId: UserWalletId, value: YieldBoostPromo) {
        dataStore.update(emptyMap()) { current ->
            current + (userWalletId to value)
        }
    }
}