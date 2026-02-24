package com.tangem.feature.wallet.child.organizetokens.model.common

import com.tangem.feature.wallet.child.organizetokens.entity.DraggableItem
import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeTokensListUM
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

internal inline fun OrganizeTokensListUM.updateItems(
    update: (PersistentList<DraggableItem>) -> List<DraggableItem>,
): OrganizeTokensListUM {
    val updatedItems = update(items).toPersistentList()

    return when (this) {
        is OrganizeTokensListUM.AccountList -> copy(items = updatedItems)
        is OrganizeTokensListUM.TokensList -> copy(items = updatedItems)
        OrganizeTokensListUM.EmptyList -> this
    }
}