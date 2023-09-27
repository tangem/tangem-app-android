package com.tangem.feature.wallet.presentation.organizetokens.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal sealed class OrganizeTokensListState {
    abstract val items: PersistentList<DraggableItem>

    data class GroupedByNetwork(
        override val items: PersistentList<DraggableItem>,
    ) : OrganizeTokensListState()

    data class Ungrouped(
        override val items: PersistentList<DraggableItem>,
    ) : OrganizeTokensListState()

    object Empty : OrganizeTokensListState() {
        override val items: PersistentList<DraggableItem> = persistentListOf()
    }

    fun copySealed(items: PersistentList<DraggableItem> = this.items): OrganizeTokensListState {
        return when (this) {
            is GroupedByNetwork -> copy(items = items)
            is Ungrouped -> copy(items = items)
            is Empty -> Empty
        }
    }
}
