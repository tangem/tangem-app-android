package com.tangem.features.nft.collections.entity

internal data class NFTCollectionAssetUM(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val price: NFTSalePriceUM,
    val onItemClick: () -> Unit,
)