package com.tangem.feature.wallet.child.organizetokens.model.common

import com.tangem.feature.wallet.child.organizetokens.entity.DraggableItem

internal fun getGroupPlaceholderLegacy(index: Int, accountId: String = ""): DraggableItem.Placeholder {
    return DraggableItem.Placeholder(
        id = "placeholder_${accountId}_${index.inc()}",
        accountId = accountId,
    )
}