package com.tangem.feature.wallet.child.organizetokens.model.common

import com.tangem.feature.wallet.child.organizetokens.entity.DraggableItem
import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeRowItemUM

internal fun getGroupPlaceholderLegacy(index: Int, accountId: String = ""): DraggableItem.Placeholder {
    return DraggableItem.Placeholder(
        id = "placeholder_${accountId}_${index.inc()}",
        accountId = accountId,
    )
}

internal fun getGroupPlaceholder(index: Int, accountId: String = ""): OrganizeRowItemUM.Placeholder {
    return OrganizeRowItemUM.Placeholder(
        id = "placeholder_${accountId}_${index.inc()}",
        accountId = accountId,
    )
}