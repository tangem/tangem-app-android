package com.tangem.domain.nft.repository

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.nft.models.NFTAsset
import com.tangem.domain.nft.models.NFTCollection
import com.tangem.domain.nft.models.NFTCollections
import com.tangem.domain.nft.models.NFTSalePrice
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

interface NFTRepository {
    fun observeCollections(userWalletId: UserWalletId, networks: List<Network>): Flow<List<NFTCollections>>

    fun getNFTCurrency(network: Network): CryptoCurrency

    suspend fun getNFTSalePrice(
        userWalletId: UserWalletId,
        network: Network,
        collectionId: NFTCollection.Identifier,
        assetId: NFTAsset.Identifier,
    ): NFTSalePrice

    suspend fun refreshCollections(userWalletId: UserWalletId, networks: List<Network>)

    suspend fun refreshAssets(userWalletId: UserWalletId, network: Network, collectionId: NFTCollection.Identifier)

    suspend fun refreshAll(userWalletId: UserWalletId, networks: List<Network>)

    suspend fun isNFTSupported(userWalletId: UserWalletId, network: Network): Boolean

    suspend fun getNFTSupportedNetworks(userWalletId: UserWalletId): List<Network>

    suspend fun getNFTExploreUrl(network: Network, assetIdentifier: NFTAsset.Identifier): String?

    suspend fun clearCache(userWalletId: UserWalletId, networks: List<Network>)
}