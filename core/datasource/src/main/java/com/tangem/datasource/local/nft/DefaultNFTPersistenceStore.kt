package com.tangem.datasource.local.nft

import androidx.datastore.core.DataStore
import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.blockchain.nft.models.NFTCollection
import com.tangem.datasource.local.nft.custom.NFTPriceId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

internal class DefaultNFTPersistenceStore(
    private val collectionsPersistenceStore: DataStore<List<NFTCollection>>,
    private val pricesPersistenceStore: DataStore<List<NFTPriceId>>,
) : NFTPersistenceStore {

    override fun getCollections(): Flow<List<NFTCollection>?> = collectionsPersistenceStore.data

    override suspend fun getCollectionsSync(): List<NFTCollection>? = collectionsPersistenceStore
        .data
        .firstOrNull()

    override fun getAsset(collectionId: NFTCollection.Identifier, assetId: NFTAsset.Identifier): Flow<NFTAsset?> =
        collectionsPersistenceStore.data
            .map { collection ->
                collection
                    .firstOrNull { it.identifier == collectionId }
                    ?.getAsset(assetId)
            }

    override fun getSalePrice(assetId: NFTAsset.Identifier): Flow<NFTAsset.SalePrice?> = pricesPersistenceStore.data
        .map { data -> data.associate { it.assetId to it.price }[assetId] }

    override suspend fun getSalePricesSync(): Map<NFTAsset.Identifier, NFTAsset.SalePrice>? = pricesPersistenceStore
        .data
        .firstOrNull()
        ?.associate { it.assetId to it.price }

    override suspend fun saveCollections(collections: List<NFTCollection>) {
        collectionsPersistenceStore.updateData {
            collections
        }
    }

    override suspend fun saveSalePrice(assetId: NFTAsset.Identifier, salePrice: NFTAsset.SalePrice) {
        pricesPersistenceStore.updateData {
            it.toMutableList() + NFTPriceId(assetId = assetId, price = salePrice)
        }
    }

    override suspend fun clear() {
        collectionsPersistenceStore.updateData { emptyList() }
        pricesPersistenceStore.updateData { emptyList() }
    }

    private fun NFTCollection.getAsset(assetId: NFTAsset.Identifier) = assets.firstOrNull { it.identifier == assetId }
}