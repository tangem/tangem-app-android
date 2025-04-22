package com.tangem.features.nft.receive.entity

import com.tangem.core.ui.components.rows.model.ChainRowUM

internal data class NFTNetworkUM(
    val id: String,
    val chainRowUM: ChainRowUM,
    val onItemClick: () -> Unit,
)