package com.tangem.domain.nft.models

import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.domain.models.StatusSource

sealed class NFTCollectionItem {
    abstract val assetId: NFTAsset.Identifier

    data class Value(
        override val assetId: NFTAsset.Identifier,
        val asset: NFTAsset,
        val salePrice: NFTSalePrice,
        val status: StatusSource,
    ) : NFTCollectionItem()

    data class Error(
        override val assetId: NFTAsset.Identifier,
    ) : NFTCollectionItem()
}
