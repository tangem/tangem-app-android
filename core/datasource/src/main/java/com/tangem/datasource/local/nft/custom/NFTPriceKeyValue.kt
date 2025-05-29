package com.tangem.datasource.local.nft.custom

import com.tangem.blockchain.nft.models.NFTAsset

data class NFTPriceKeyValue(
    val key: NFTAsset.Identifier,
    val value: NFTAsset.SalePrice,
)