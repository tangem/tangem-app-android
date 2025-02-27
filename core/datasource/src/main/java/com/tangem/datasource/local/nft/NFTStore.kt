package com.tangem.datasource.local.nft

import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.blockchain.nft.models.NFTCollection
import com.tangem.domain.nft.models.NFTCollectionItem
import com.tangem.domain.nft.models.NFTCollections
import com.tangem.domain.nft.models.NFTSalePrice
import kotlinx.coroutines.flow.Flow

interface NFTStore {
    fun getCollections(): Flow<NFTCollections>

    fun getAsset(collectionId: NFTCollection.Identifier, assetId: NFTAsset.Identifier): Flow<NFTCollectionItem?>

    fun getSalePrice(assetId: NFTAsset.Identifier): Flow<NFTSalePrice?>

    suspend fun saveCollections(collectionsData: NFTCollections)

    suspend fun saveSalePrice(salePrice: NFTSalePrice)
}
