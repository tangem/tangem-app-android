package com.tangem.features.nft.details.entity

import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig

internal data class NFTDetailsUM(
    val nftAsset: NFTAssetUM,
    val pullToRefreshConfig: PullToRefreshConfig,
    val onBackClick: () -> Unit,
    val onReadMoreClick: () -> Unit,
    val onSeeAllTraitsClick: () -> Unit,
    val onExploreClick: () -> Unit,
    val onSendClick: () -> Unit,
)