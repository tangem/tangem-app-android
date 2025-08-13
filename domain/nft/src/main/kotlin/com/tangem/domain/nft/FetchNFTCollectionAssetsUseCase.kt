package com.tangem.domain.nft

import com.tangem.domain.models.network.Network
import com.tangem.domain.nft.models.NFTCollection
import com.tangem.domain.nft.repository.NFTRepository
import com.tangem.domain.models.wallet.UserWalletId

class FetchNFTCollectionAssetsUseCase(
    private val nftRepository: NFTRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, network: Network, collectionId: NFTCollection.Identifier) {
        nftRepository.refreshAssets(userWalletId, network, collectionId)
    }
}