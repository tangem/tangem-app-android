package com.tangem.domain.nft.repository

import com.tangem.domain.nft.models.NFTAsset
import com.tangem.domain.nft.models.NFTCollection
import com.tangem.domain.nft.models.NFTCollections
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

interface NFTRepository {
    fun observeCollections(userWalletId: UserWalletId, networks: List<Network>): Flow<List<NFTCollections>>

    suspend fun refreshCollections(userWalletId: UserWalletId, networks: List<Network>)

    suspend fun refreshAssets(userWalletId: UserWalletId, network: Network, collectionId: NFTCollection.Identifier)

    suspend fun refreshAll(userWalletId: UserWalletId, networks: List<Network>)

    suspend fun isNFTSupported(network: Network): Boolean

    suspend fun getNFTExploreUrl(network: Network, assetIdentifier: NFTAsset.Identifier): String?
}