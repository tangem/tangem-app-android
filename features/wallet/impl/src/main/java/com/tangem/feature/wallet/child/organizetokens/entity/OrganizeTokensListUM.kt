package com.tangem.feature.wallet.child.organizetokens.entity

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal sealed interface OrganizeTokensListUM {

    val items: PersistentList<DraggableItem>
    val isGrouped: Boolean

    data class AccountList(
        override val items: PersistentList<DraggableItem>,
        override val isGrouped: Boolean,
    ) : OrganizeTokensListUM

    data class TokensList(
        override val items: PersistentList<DraggableItem>,
        override val isGrouped: Boolean,
    ) : OrganizeTokensListUM

    data object EmptyList : OrganizeTokensListUM {
        override val items: PersistentList<DraggableItem> = persistentListOf()
        override val isGrouped: Boolean = false
    }
}