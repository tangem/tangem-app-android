package com.tangem.domain.nft.models

import com.tangem.domain.core.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

@Serializable
sealed class NFTSalePrice {
    abstract val assetId: NFTAsset.Identifier

    @Serializable
    data class Empty(
        override val assetId: NFTAsset.Identifier,
    ) : NFTSalePrice()

    @Serializable
    data class Loading(
        override val assetId: NFTAsset.Identifier,
    ) : NFTSalePrice()

    @Serializable
    data class Error(
        override val assetId: NFTAsset.Identifier,
    ) : NFTSalePrice()

    @Serializable
    data class Value(
        override val assetId: NFTAsset.Identifier,
        val value: SerializedBigDecimal,
        val fiatValue: SerializedBigDecimal?,
        val symbol: String,
        val decimals: Int,
    ) : NFTSalePrice()
}