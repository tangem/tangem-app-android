package com.tangem.features.details.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.block.model.BlockUM
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
            val block: BlockUM,
        )
    }

    data class WalletConnect(val onClick: () -> Unit) : DetailsItemUM() {
        override val id: String = "wallet_connect"
    }

    data object UserWalletList : DetailsItemUM() {
        override val id: String = "user_wallet_list"
    }
}
