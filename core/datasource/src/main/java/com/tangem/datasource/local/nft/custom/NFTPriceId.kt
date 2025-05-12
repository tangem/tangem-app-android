package com.tangem.datasource.local.nft.custom

import com.tangem.blockchain.nft.models.NFTAsset

data class NFTPriceId(
    val assetId: NFTAsset.Identifier,
    val price: NFTAsset.SalePrice,
)