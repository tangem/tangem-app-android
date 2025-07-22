package com.tangem.datasource.local.token

import androidx.datastore.core.DataStore
import com.tangem.datasource.api.express.models.response.Asset
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

internal typealias AssetsByWalletId = Map<String, List<Asset>>

internal class DefaultExpressAssetsStore(
    private val persistenceStore: DataStore<AssetsByWalletId>,
    private val runtimeStore: StringKeyDataStore<List<Asset>>,
) : ExpressAssetsStore {

    override suspend fun getSyncOrNull(userWalletId: UserWalletId): List<Asset>? {
        val runtimeAssets = runtimeStore.getSyncOrNull(userWalletId.stringValue)

        if (runtimeAssets != null) {
            return runtimeAssets
        }

        val cachedAssets = getCachedAssets(userWalletId)

        return if (cachedAssets != null) {
            runtimeStore.store(userWalletId.stringValue, cachedAssets)
            cachedAssets
        } else {
            null
        }
    }

    private suspend fun getCachedAssets(userWalletId: UserWalletId): List<Asset>? {
        return persistenceStore.data.firstOrNull().orEmpty()[userWalletId.stringValue]
    }

    override suspend fun store(userWalletId: UserWalletId, item: List<Asset>) {
        coroutineScope {
            launch {
                runtimeStore.store(userWalletId.stringValue, item)
            }
            launch {
                persistenceStore.updateData {
                    it.toMutableMap().apply {
                        put(userWalletId.stringValue, item)
                    }
                }
            }
        }
    }
}