package com.tangem.datasource.local.token

import com.tangem.datasource.api.express.models.response.Asset
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.domain.wallets.models.UserWalletId

internal class DefaultExpressAssetsStore(
    private val dataStore: StringKeyDataStore<List<Asset>>,
) : ExpressAssetsStore {

    override suspend fun getSyncOrNull(userWalletId: UserWalletId): List<Asset>? {
        return dataStore.getSyncOrNull(userWalletId.stringValue)
    }

    override suspend fun store(userWalletId: UserWalletId, item: List<Asset>) {
        dataStore.store(userWalletId.stringValue, item)
    }
}
