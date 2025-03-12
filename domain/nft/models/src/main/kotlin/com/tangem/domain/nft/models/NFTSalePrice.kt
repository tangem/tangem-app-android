package com.tangem.domain.nft.models

import com.tangem.domain.models.StatusSource
import java.math.BigDecimal

sealed class NFTSalePrice {
    abstract val assetId: NFTAsset.Identifier

    data class Empty(
        override val assetId: NFTAsset.Identifier,
    ) : NFTSalePrice()

    data class Error(
        override val assetId: NFTAsset.Identifier,
    ) : NFTSalePrice()

    data class Value(
        override val assetId: NFTAsset.Identifier,
        val value: BigDecimal,
        val symbol: String,
        val source: StatusSource,
    ) : NFTSalePrice()
}