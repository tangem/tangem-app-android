package com.tangem.features.nft.details.entity

internal data class NFTDetailsUM(
    val nftAsset: NFTAssetUM,
    val onBackClick: () -> Unit,
    val onReadMoreClick: () -> Unit,
    val onSeeAllTraitsClick: () -> Unit,
    val onExploreClick: () -> Unit,
    val onSendClick: () -> Unit,
)