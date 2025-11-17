package com.tangem.feature.wallet.presentation.organizetokens.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Deprecated("Use OrganizeTokensListUM instead, will be removed in future releases")
@Immutable
internal sealed class OrganizeTokensListState {
    abstract val items: PersistentList<DraggableItem>

    data class GroupedByNetwork(
        override val items: PersistentList<DraggableItem>,
    ) : OrganizeTokensListState()

    data class Ungrouped(
        override val items: PersistentList<DraggableItem>,
    ) : OrganizeTokensListState()

    data object Empty : OrganizeTokensListState() {
        override val items: PersistentList<DraggableItem> = persistentListOf()
    }
}

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