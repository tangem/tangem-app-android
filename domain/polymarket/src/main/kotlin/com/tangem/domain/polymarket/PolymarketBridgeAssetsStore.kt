package com.tangem.domain.polymarket

import com.tangem.domain.polymarket.model.PolymarketBridgeAsset

interface PolymarketBridgeAssetsStore {

    suspend fun getSyncOrNull(): List<PolymarketBridgeAsset>?

    suspend fun store(value: List<PolymarketBridgeAsset>)
}