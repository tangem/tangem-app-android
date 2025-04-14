package com.tangem.features.nft.collections.entity

internal data class NFTCollectionsStateUM(
    val onBackClick: () -> Unit,
    val content: NFTCollectionsUM,
)