package com.tangem.data.nft.custom

import com.tangem.blockchain.nft.models.NFTAsset

internal data class NFTPriceId(
    val assetId: NFTAsset.Identifier,
    val price: NFTAsset.SalePrice,
)