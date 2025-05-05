package com.tangem.features.walletconnect.connections.entity

import androidx.annotation.DrawableRes

data class WcNetworkInfoItem(
    val id: String,
    @DrawableRes val icon: Int,
    val name: String,
    val symbol: String,
)