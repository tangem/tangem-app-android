package com.tangem.features.nft.collections.entity

import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig

internal data class NFTCollectionsStateUM(
    val onBackClick: () -> Unit,
    val pullToRefreshConfig: PullToRefreshConfig,
    val content: NFTCollectionsUM,
)