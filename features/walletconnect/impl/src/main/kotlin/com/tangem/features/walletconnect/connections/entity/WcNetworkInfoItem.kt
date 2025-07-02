package com.tangem.features.walletconnect.connections.entity

import androidx.annotation.DrawableRes

sealed class WcNetworkInfoItem {
    abstract val id: String

    @get:DrawableRes
    abstract val icon: Int
    abstract val name: String
    abstract val symbol: String

    data class Required(
        override val id: String,
        override val icon: Int,
        override val name: String,
        override val symbol: String,
    ) : WcNetworkInfoItem()

    data class Checked(
        override val id: String,
        override val icon: Int,
        override val name: String,
        override val symbol: String,
    ) : WcNetworkInfoItem()

    data class Checkable(
        override val id: String,
        override val icon: Int,
        override val name: String,
        override val symbol: String,
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit,
    ) : WcNetworkInfoItem()

    data class ReadOnly(
        override val id: String,
        override val icon: Int,
        override val name: String,
        override val symbol: String,
    ) : WcNetworkInfoItem()
}