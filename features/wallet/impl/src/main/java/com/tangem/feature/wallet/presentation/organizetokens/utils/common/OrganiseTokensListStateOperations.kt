package com.tangem.feature.wallet.presentation.organizetokens.utils.common

import com.tangem.feature.wallet.presentation.organizetokens.DraggableItem
import com.tangem.feature.wallet.presentation.organizetokens.OrganizeTokensListState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

@Suppress("UNCHECKED_CAST")
internal inline fun OrganizeTokensListState.updateItems(
    update: (PersistentList<DraggableItem>) -> List<DraggableItem>,
): OrganizeTokensListState {
    val updatedItems = update(items).toPersistentList()

    return when (this) {
        is OrganizeTokensListState.GroupedByNetwork -> copy(items = updatedItems)
        is OrganizeTokensListState.Ungrouped -> copy(items = updatedItems as PersistentList<DraggableItem.Token>)
        is OrganizeTokensListState.Empty -> this
    }
}