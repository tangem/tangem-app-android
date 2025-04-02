package com.tangem.features.nft.receive.entity

import androidx.annotation.DrawableRes

internal data class NFTNetworkUM(
    val id: String,
    @DrawableRes val iconRes: Int,
    val name: String,
    val onItemClick: () -> Unit,
)