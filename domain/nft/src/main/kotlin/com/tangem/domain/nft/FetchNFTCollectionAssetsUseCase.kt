package com.tangem.domain.nft

import com.tangem.domain.nft.models.NFTCollection
import com.tangem.domain.nft.repository.NFTRepository
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId

class FetchNFTCollectionAssetsUseCase(
    private val nftRepository: NFTRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, network: Network, collectionId: NFTCollection.Identifier) {
        nftRepository.refreshAssets(userWalletId, network, collectionId)
    }
}