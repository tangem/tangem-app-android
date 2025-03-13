package com.tangem.datasource.local.nft

import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.nft.models.NFTAsset
import com.tangem.domain.nft.models.NFTCollection
import com.tangem.domain.nft.models.NFTCollections
import com.tangem.domain.nft.models.NFTSalePrice
import com.tangem.domain.tokens.model.Network
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

internal class DefaultNFTRuntimeStore(
    private val network: Network,
    private val collectionsRuntimeStore: RuntimeSharedStore<NFTCollections>,
    private val pricesRuntimeStore: RuntimeSharedStore<Map<NFTAsset.Identifier, NFTSalePrice>>,
) : NFTRuntimeStore {

    override suspend fun initialize(collections: NFTCollections, prices: Map<NFTAsset.Identifier, NFTSalePrice>) {
        collectionsRuntimeStore.store(collections)
        pricesRuntimeStore.store(prices)
    }

    override fun getCollections(): Flow<NFTCollections> = collectionsRuntimeStore
        .get()
        .combine(pricesRuntimeStore.get(), ::Pair)
        .map {
            val (collectionsData, prices) = it
            collectionsData.mergeWithPrices(prices)
        }

    override suspend fun getCollectionsSync(): NFTCollections {
        val collectionsData = collectionsRuntimeStore.getSyncOrNull()
        val prices = pricesRuntimeStore.getSyncOrNull()
        return collectionsData
            ?.mergeWithPrices(prices.orEmpty())
            ?: NFTCollections(
                network = network,
                content = NFTCollections.Content.Collections(
                    collections = null,
                    source = StatusSource.CACHE,
                ),
            )
    }

    override fun getAsset(collectionId: NFTCollection.Identifier, assetId: NFTAsset.Identifier): Flow<NFTAsset> =
        collectionsRuntimeStore
            .get()
            .combine(getSalePrice(assetId), ::Pair)
            .map {
                val (collectionsData, price) = it
                collectionsData
                    .getCollection(collectionId)
                    ?.getAsset(assetId)
                    ?.mergeWithPrice(price)
                    ?: NFTAsset.Error(assetId)
            }

    override fun getSalePrice(assetId: NFTAsset.Identifier): Flow<NFTSalePrice> = pricesRuntimeStore
        .get()
        .map { it[assetId] ?: NFTSalePrice.Empty(assetId) }

    override suspend fun saveCollections(collections: NFTCollections) {
        collectionsRuntimeStore.store(collections)
    }

    override suspend fun saveSalePrice(salePrice: NFTSalePrice) {
        pricesRuntimeStore.update(emptyMap()) {
            it.plus(salePrice.assetId to salePrice)
        }
    }

    private fun NFTCollections.getCollection(collectionId: NFTCollection.Identifier): NFTCollection? =
        (content as? NFTCollections.Content.Collections)
            ?.collections
            ?.firstOrNull { it.id == collectionId }

    private fun NFTCollection.getAsset(assetId: NFTAsset.Identifier): NFTAsset? =
        assets.firstOrNull { it.id == assetId }

    private fun NFTCollections.mergeWithPrices(prices: Map<NFTAsset.Identifier, NFTSalePrice>): NFTCollections =
        when (val content = this.content) {
            is NFTCollections.Content.Collections -> {
                this.copy(
                    content = content.mergeWithPrices(prices),
                )
            }
            is NFTCollections.Content.Error -> this
        }

    private fun NFTCollections.Content.Collections.mergeWithPrices(prices: Map<NFTAsset.Identifier, NFTSalePrice>) =
        copy(
            collections = this.collections?.map { data ->
                data.copy(
                    assets = data.assets.map { asset ->
                        asset.mergeWithPrice(prices[asset.id] ?: NFTSalePrice.Empty(asset.id))
                    },
                )
            },
            source = this.source,
        )

    private fun NFTAsset.mergeWithPrice(price: NFTSalePrice): NFTAsset = when (this) {
        is NFTAsset.Error -> this
        is NFTAsset.Value -> copy(
            salePrice = price,
        )
    }
}