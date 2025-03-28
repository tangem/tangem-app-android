package com.tangem.datasource.local.nft

import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.blockchain.nft.models.NFTCollection
import kotlinx.coroutines.flow.Flow

interface NFTPersistenceStore {
    fun getCollections(): Flow<List<NFTCollection>?>

    suspend fun getCollectionsSync(): List<NFTCollection>?

    fun getAsset(collectionId: NFTCollection.Identifier, assetId: NFTAsset.Identifier): Flow<NFTAsset?>

    fun getSalePrice(assetId: NFTAsset.Identifier): Flow<NFTAsset.SalePrice?>

    suspend fun getSalePricesSync(): Map<NFTAsset.Identifier, NFTAsset.SalePrice>?

    suspend fun saveCollections(collections: List<NFTCollection>)

    suspend fun saveSalePrice(assetId: NFTAsset.Identifier, salePrice: NFTAsset.SalePrice)
}