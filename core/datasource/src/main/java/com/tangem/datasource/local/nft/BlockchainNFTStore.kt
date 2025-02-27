package com.tangem.datasource.local.nft

import androidx.datastore.core.DataStore
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.blockchain.nft.models.NFTCollection
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.nft.models.NFTCollectionItem
import com.tangem.domain.nft.models.NFTCollections
import com.tangem.domain.nft.models.NFTCollections.Collection
import com.tangem.domain.nft.models.NFTSalePrice
import kotlinx.coroutines.flow.*

internal class BlockchainNFTStore(
    private val blockchain: Blockchain,
    private val collectionsPersistenceStore: DataStore<List<NFTCollection>>,
    private val collectionsRuntimeStore: RuntimeSharedStore<NFTCollections>,
    private val pricesPersistenceStore: DataStore<Map<NFTAsset.Identifier, NFTAsset.SalePrice>>,
    private val pricesRuntimeStore: RuntimeSharedStore<Map<NFTAsset.Identifier, NFTSalePrice>>,
) : NFTStore {

    private var runtimeCacheInitialized: Boolean = false

    override fun getCollections(): Flow<NFTCollections> = flow {
        initRuntimeCacheIfNeed()
        emitAll(
            collectionsRuntimeStore.get()
                .combine(pricesRuntimeStore.get(), ::Pair)
                .map {
                    val (collectionsData, prices) = it
                    collectionsData.mergeWithPrices(prices)
                },
        )
    }

    override fun getAsset(
        collectionId: NFTCollection.Identifier,
        assetId: NFTAsset.Identifier,
    ): Flow<NFTCollectionItem> = flow {
        initRuntimeCacheIfNeed()
        emitAll(
            collectionsRuntimeStore.get()
                .combine(getSalePrice(assetId), ::Pair)
                .map {
                    val (collectionsData, price) = it
                    collectionsData
                        .getCollection(collectionId)
                        ?.getAsset(assetId)
                        ?.mergeWithPrice(price)
                        ?: NFTCollectionItem.Error(assetId)
                },
        )
    }

    override fun getSalePrice(assetId: NFTAsset.Identifier): Flow<NFTSalePrice> = flow {
        initRuntimeCacheIfNeed()
        emitAll(
            pricesRuntimeStore.get().map { it[assetId] ?: NFTSalePrice.Empty(assetId) },
        )
    }

    override suspend fun saveCollections(collectionsData: NFTCollections) {
        initRuntimeCacheIfNeed()
        when (collectionsData) {
            is NFTCollections.Error -> {
                collectionsRuntimeStore.store(collectionsData)
            }
            is NFTCollections.Value -> {
                collectionsPersistenceStore.updateData {
                    collectionsData
                        .collections
                        .filterIsInstance<Collection.Value>()
                        .map { it.collection }
                        .filter { it.count > 0 }
                }
                collectionsRuntimeStore.store(
                    collectionsData.copy(
                        collections = collectionsData
                            .collections
                            .filter {
                                it is Collection.Error ||
                                    it is Collection.Value && it.collection.count > 0
                            },
                    ),
                )
            }
        }
    }

    override suspend fun saveSalePrice(salePrice: NFTSalePrice) {
        initRuntimeCacheIfNeed()
        val assetId = salePrice.assetId
        if (salePrice is NFTSalePrice.Value) {
            pricesPersistenceStore.updateData {
                val updatedData = it.toMutableMap().apply { this[assetId] = salePrice.price }
                updatedData
            }
        }

        pricesRuntimeStore.update(emptyMap()) {
            it.plus(assetId to salePrice)
        }
    }

    private suspend fun initRuntimeCacheIfNeed() {
        if (!runtimeCacheInitialized) {
            initCollectionsRuntimeCache()
            initSalePricesRuntimeCache()
            runtimeCacheInitialized = true
        }
    }

    private suspend fun initCollectionsRuntimeCache() {
        collectionsRuntimeStore.store(
            getPersistenceCollectionsSync()
                .let { rawCollections ->
                    NFTCollections.Value(
                        blockchain = blockchain,
                        collections = rawCollections.map { rawCollection ->
                            Collection.Value(
                                collection = rawCollection,
                                source = StatusSource.CACHE,
                                salePrices = getPersistencePricesSync(),
                            )
                        },
                    )
                },
        )
    }

    private suspend fun initSalePricesRuntimeCache() {
        pricesRuntimeStore.store(getPersistencePricesSync())
    }

    private suspend fun getPersistenceCollectionsSync(): List<NFTCollection> = collectionsPersistenceStore
        .data
        .firstOrNull()
        .orEmpty()

    private suspend fun getPersistencePricesSync(): Map<NFTAsset.Identifier, NFTSalePrice.Value> =
        pricesPersistenceStore
            .data
            .firstOrNull()
            .orEmpty()
            .mapValues {
                NFTSalePrice.Value(
                    assetId = it.key,
                    price = it.value,
                    status = StatusSource.CACHE,
                )
            }

    private fun NFTCollections.getCollection(collectionId: NFTCollection.Identifier): NFTCollection? =
        (this as? NFTCollections.Value)
            ?.collections
            ?.filterIsInstance<Collection.Value>()
            .orEmpty()
            .firstOrNull { it.collection.identifier == collectionId }
            ?.collection

    private fun NFTCollection.getAsset(assetId: NFTAsset.Identifier) = assets.firstOrNull { it.identifier == assetId }

    private fun NFTCollections.mergeWithPrices(prices: Map<NFTAsset.Identifier, NFTSalePrice>): NFTCollections =
        when (this) {
            is NFTCollections.Error -> this
            is NFTCollections.Value -> this.copy(
                collections = this.collections.map { data ->
                    when (data) {
                        is Collection.Error -> data
                        is Collection.Value -> data.copy(
                            salePrices = prices,
                        )
                    }
                },
            )
        }

    private fun NFTAsset.mergeWithPrice(price: NFTSalePrice): NFTCollectionItem = NFTCollectionItem.Value(
        assetId = this.identifier,
        asset = this,
        salePrice = price,
        status = StatusSource.ACTUAL,
    )
}
