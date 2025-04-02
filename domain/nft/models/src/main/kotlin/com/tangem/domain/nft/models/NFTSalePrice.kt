package com.tangem.domain.nft.models

import java.math.BigDecimal

sealed class NFTSalePrice {
    abstract val assetId: NFTAsset.Identifier

    data class Empty(
        override val assetId: NFTAsset.Identifier,
    ) : NFTSalePrice()

    data class Loading(
        override val assetId: NFTAsset.Identifier,
    ) : NFTSalePrice()

    data class Error(
        override val assetId: NFTAsset.Identifier,
    ) : NFTSalePrice()

    data class Value(
        override val assetId: NFTAsset.Identifier,
        val value: BigDecimal,
        val symbol: String,
    ) : NFTSalePrice()
}