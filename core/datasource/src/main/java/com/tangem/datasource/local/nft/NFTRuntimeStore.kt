package com.tangem.datasource.local.nft

import com.tangem.domain.nft.models.NFTAsset
import com.tangem.domain.nft.models.NFTCollection
import com.tangem.domain.nft.models.NFTCollections
import com.tangem.domain.nft.models.NFTSalePrice
import kotlinx.coroutines.flow.Flow

interface NFTRuntimeStore {

    suspend fun initialize(collections: NFTCollections, prices: Map<NFTAsset.Identifier, NFTSalePrice>)

    fun getCollections(): Flow<NFTCollections>

    suspend fun getCollectionsSync(): NFTCollections

    fun getAsset(collectionId: NFTCollection.Identifier, assetId: NFTAsset.Identifier): Flow<NFTAsset?>

    fun getSalePrice(assetId: NFTAsset.Identifier): Flow<NFTSalePrice?>

    suspend fun saveCollections(collections: NFTCollections)

    suspend fun saveSalePrice(salePrice: NFTSalePrice)
}