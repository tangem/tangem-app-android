package com.tangem.domain.nft.models

import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.domain.models.StatusSource

sealed class NFTSalePrice {
    abstract val assetId: NFTAsset.Identifier

    data class Empty(
        override val assetId: NFTAsset.Identifier,
    ) : NFTSalePrice()

    data class Value(
        override val assetId: NFTAsset.Identifier,
        val price: NFTAsset.SalePrice,
        val status: StatusSource,
    ) : NFTSalePrice()

    data class Error(
        override val assetId: NFTAsset.Identifier,
    ) : NFTSalePrice()
}
