package com.tangem.data.polymarket.store

import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.domain.polymarket.PolymarketBridgeAssetsStore
import com.tangem.domain.polymarket.model.PolymarketBridgeAsset
import javax.inject.Inject

internal class DefaultPolymarketBridgeAssetsStore @Inject constructor() : PolymarketBridgeAssetsStore {

    private val runtimeStore = RuntimeSharedStore<List<PolymarketBridgeAsset>>()

    override suspend fun getSyncOrNull(): List<PolymarketBridgeAsset>? = runtimeStore.getSyncOrNull()

    override suspend fun store(value: List<PolymarketBridgeAsset>) {
        runtimeStore.store(value)
    }
}