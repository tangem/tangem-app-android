package com.tangem.datasource.local.token

import com.tangem.datasource.api.tangemTech.models.CoinsResponse
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.domain.wallets.models.UserWalletId

internal class DefaultUserMarketCoinsStore(
    private val dataStore: StringKeyDataStore<CoinsResponse>,
) : UserMarketCoinsStore {

    override suspend fun getSyncOrNull(userWalletId: UserWalletId): CoinsResponse? {
        return dataStore.getSyncOrNull(userWalletId.stringValue)
    }

    override suspend fun store(userWalletId: UserWalletId, item: CoinsResponse) {
        dataStore.store(userWalletId.stringValue, item)
    }
}