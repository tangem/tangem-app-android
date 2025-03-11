package com.tangem.feature.wallet.presentation.organizetokens.utils.common

import com.tangem.feature.wallet.presentation.organizetokens.model.DraggableItem
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensListState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

internal inline fun OrganizeTokensListState.updateItems(
    update: (PersistentList<DraggableItem>) -> List<DraggableItem>,
): OrganizeTokensListState {
    val updatedItems = update(items).toPersistentList()

    return when (this) {
        is OrganizeTokensListState.GroupedByNetwork -> copy(items = updatedItems)
        is OrganizeTokensListState.Ungrouped -> copy(items = updatedItems)
        is OrganizeTokensListState.Empty -> this
    }
}