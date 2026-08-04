package com.tangem.datasource.local.token

import androidx.datastore.core.DataStore
import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.datasource.api.express.models.response.Asset
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

internal typealias AssetsByWalletId = Map<String, List<Asset>>

internal class DefaultExpressAssetsStore(
    private val persistenceStore: DataStore<AssetsByWalletId>,
    private val runtimeStore: RuntimeSharedMapStore<UserWalletId, List<Asset>>,
) : ExpressAssetsStore {

    override suspend fun getSyncOrNull(userWalletId: UserWalletId): List<Asset>? {
        val runtimeAssets = runtimeStore.getSyncOrNull(userWalletId)

        if (runtimeAssets != null) {
            return runtimeAssets
        }

        val cachedAssets = getCachedAssets(userWalletId)

        return if (cachedAssets != null) {
            runtimeStore.store(userWalletId, cachedAssets)
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
                runtimeStore.store(userWalletId, item)
            }
            launch {
                persistenceStore.updateData { assetsByWalletId ->
                    assetsByWalletId.toMutableMap().apply {
                        put(userWalletId.stringValue, item)
                    }
                }
            }
        }
    }
}