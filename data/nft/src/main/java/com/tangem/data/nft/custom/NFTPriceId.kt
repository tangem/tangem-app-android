package com.tangem.data.nft.custom

import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.data.nft.store.NFTAssetIdentifierSerializer
import com.tangem.data.nft.store.NFTSalePriceSerializer
import kotlinx.serialization.Serializable

@Serializable
internal data class NFTPriceId(
    @Serializable(with = NFTAssetIdentifierSerializer::class) val assetId: NFTAsset.Identifier,
    @Serializable(with = NFTSalePriceSerializer::class) val price: NFTAsset.SalePrice,
)