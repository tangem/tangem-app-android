package com.tangem.feature.wallet.presentation.organizetokens.utils.common

import com.tangem.feature.wallet.presentation.organizetokens.model.DraggableItem

internal fun getGroupPlaceholder(index: Int, accountId: String = ""): DraggableItem.Placeholder {
    return DraggableItem.Placeholder(
        id = "placeholder_${accountId}_${index.inc()}",
        accountId = accountId,
    )
}