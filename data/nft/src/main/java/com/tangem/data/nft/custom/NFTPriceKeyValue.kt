package com.tangem.data.nft.custom

import com.tangem.blockchain.nft.models.NFTAsset

internal data class NFTPriceKeyValue(
    val key: NFTAsset.Identifier,
    val value: NFTAsset.SalePrice,
)