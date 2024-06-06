package com.tangem.features.details.entity

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed class DetailsItemUM {

    abstract val id: String

    data class Basic(
        override val id: String,
        val items: ImmutableList<Item>,
    ) : DetailsItemUM() {

        data class Item(
            val id: String,
            val title: TextReference,
            @DrawableRes
            val iconRes: Int,
            val onClick: () -> Unit,
        )
    }

    data class WalletConnect(val onClick: () -> Unit) : DetailsItemUM() {
        override val id: String = "wallet_connect"
    }

    data object UserWalletList : DetailsItemUM() {
        override val id: String = "user_wallet_list"
    }
}